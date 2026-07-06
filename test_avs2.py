#!/usr/bin/env python3
"""测试不同 demuxer 选项是否能避免 davs2 崩溃"""
import ctypes
import os
import sys
import time
import faulthandler

faulthandler.enable()

dll_path = r'd:\github\IPTV-Scanner-Editor-Pro\mpv\libmpv-2.dll'
os.environ['MPV_HOME'] = os.path.dirname(dll_path)
os.environ['PATH'] = os.path.dirname(dll_path) + os.pathsep + os.environ.get('PATH', '')

libmpv = ctypes.CDLL(dll_path)

libmpv.mpv_create.restype = ctypes.c_void_p
libmpv.mpv_initialize.argtypes = [ctypes.c_void_p]
libmpv.mpv_initialize.restype = ctypes.c_int
libmpv.mpv_set_option_string.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]
libmpv.mpv_set_option_string.restype = ctypes.c_int
libmpv.mpv_set_property_string.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]
libmpv.mpv_set_property_string.restype = ctypes.c_int
libmpv.mpv_command.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_char_p)]
libmpv.mpv_command.restype = ctypes.c_int
libmpv.mpv_wait_event.argtypes = [ctypes.c_void_p, ctypes.c_double]
libmpv.mpv_wait_event.restype = ctypes.c_void_p
libmpv.mpv_terminate_destroy.argtypes = [ctypes.c_void_p]
libmpv.mpv_request_log_messages.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
libmpv.mpv_request_log_messages.restype = ctypes.c_int
libmpv.mpv_get_property_string.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
libmpv.mpv_get_property_string.restype = ctypes.c_void_p

MPV_EVENT_NONE = 0
MPV_EVENT_SHUTDOWN = 1
MPV_EVENT_LOG_MESSAGE = 2
MPV_EVENT_END_FILE = 7
MPV_EVENT_FILE_LOADED = 8

class mpv_event_log_message(ctypes.Structure):
    _fields_ = [
        ('prefix', ctypes.c_char_p),
        ('level', ctypes.c_char_p),
        ('text', ctypes.c_char_p),
    ]

class mpv_event_end_file(ctypes.Structure):
    _fields_ = [
        ('reason', ctypes.c_int),
        ('error', ctypes.c_int),
    ]

class mpv_event(ctypes.Structure):
    _fields_ = [
        ('event_id', ctypes.c_ulonglong),
        ('error', ctypes.c_int),
        ('reply_userdata', ctypes.c_ulonglong),
        ('data', ctypes.c_void_p),
    ]

REASON_MAP = {0: 'EOF', 2: 'STOP', 3: 'QUIT', 4: 'ERROR'}

def test(label, mpv_url, options):
    print(f"\n{'='*60}")
    print(f"Test: {label}")
    print(f"  URL: {mpv_url[:80]}")
    for k, v in options.items():
        print(f"  {k}: {v}")
    print(f"{'='*60}")

    ctx = libmpv.mpv_create()
    if not ctx:
        print("ERROR: mpv_create failed")
        return None

    libmpv.mpv_set_option_string(ctx, b'vo', b'null')
    libmpv.mpv_set_option_string(ctx, b'ao', b'null')
    libmpv.mpv_set_option_string(ctx, b'vid', b'no')
    libmpv.mpv_set_option_string(ctx, b'aid', b'no')
    libmpv.mpv_set_option_string(ctx, b'idle', b'yes')
    libmpv.mpv_set_option_string(ctx, b'keep-open', b'yes')
    libmpv.mpv_set_option_string(ctx, b'pause', b'yes')
    libmpv.mpv_set_option_string(ctx, b'msg-level', b'all=v')
    libmpv.mpv_request_log_messages(ctx, b'v')

    ret = libmpv.mpv_initialize(ctx)
    if ret < 0:
        print(f"ERROR: mpv_initialize failed: {ret}")
        return None

    for k, v in options.items():
        r = libmpv.mpv_set_property_string(ctx, k.encode(), v.encode())
        if r < 0:
            print(f"  WARNING: set {k}={v} failed: {r}")

    cmd = (ctypes.c_char_p * 3)(b'loadfile', mpv_url.encode('utf-8'), None)
    ret = libmpv.mpv_command(ctx, cmd)
    print(f"loadfile returned: {ret}")

    start_time = time.time()
    file_loaded = False
    end_file_reason = None
    end_file_error = None

    while time.time() - start_time < 15:
        try:
            event_ptr = libmpv.mpv_wait_event(ctx, 0.5)
            if not event_ptr:
                continue
            event = ctypes.cast(event_ptr, ctypes.POINTER(mpv_event)).contents

            if event.event_id == MPV_EVENT_NONE:
                continue

            if event.event_id == MPV_EVENT_LOG_MESSAGE:
                if event.data:
                    try:
                        log_msg = ctypes.cast(event.data, ctypes.POINTER(mpv_event_log_message)).contents
                        prefix = log_msg.prefix.decode('utf-8', errors='ignore') if log_msg.prefix else ''
                        level = log_msg.level.decode('utf-8', errors='ignore') if log_msg.level else ''
                        text = log_msg.text.decode('utf-8', errors='ignore').rstrip() if log_msg.text else ''
                        if text:
                            print(f"  [{prefix}/{level}] {text}")
                    except Exception:
                        pass

            elif event.event_id == MPV_EVENT_FILE_LOADED:
                file_loaded = True
                print("  >>> EVENT: FILE_LOADED")
                for prop in [b'demuxer', b'video-codec', b'audio-codec', b'file-format',
                             b'track-list/count', b'width', b'height']:
                    p = libmpv.mpv_get_property_string(ctx, prop)
                    if p:
                        v = ctypes.cast(p, ctypes.c_char_p).value
                        if v:
                            print(f"    {prop.decode()}: {v.decode('utf-8', errors='ignore')}")
                break

            elif event.event_id == MPV_EVENT_END_FILE:
                if event.data:
                    try:
                        end_file = ctypes.cast(event.data, ctypes.POINTER(mpv_event_end_file)).contents
                        end_file_reason = end_file.reason
                        end_file_error = end_file.error
                        print(f"  >>> EVENT: END_FILE reason={end_file.reason}({REASON_MAP.get(end_file.reason, '?')}) error={end_file_error}")
                    except Exception:
                        pass
                break

            elif event.event_id == MPV_EVENT_SHUTDOWN:
                print("  >>> EVENT: SHUTDOWN")
                break

        except Exception as e:
            print(f"  Event loop error: {e}")
            break

    elapsed = time.time() - start_time
    print(f"\n  Result: loaded={file_loaded}, reason={end_file_reason}, error={end_file_error}, time={elapsed:.1f}s")

    try:
        libmpv.mpv_terminate_destroy(ctx)
    except Exception:
        pass

    return file_loaded

avs2_file = r'D:\桌面\4K_AVS2_AAC5.1.ts'

# 测试1：analyzeduration=0（跳过流信息分析中的解码）
test("Test 1: analyzeduration=0", avs2_file, {
    'demuxer': '',
    'demuxer-lavf-format': '',
    'demuxer-lavf-probesize': '5000000',
    'demuxer-lavf-analyzeduration': '0',
    'cache': 'no',
})

# 测试2：probesize=0
test("Test 2: probesize=0", avs2_file, {
    'demuxer': '',
    'demuxer-lavf-format': '',
    'demuxer-lavf-probesize': '0',
    'demuxer-lavf-analyzeduration': '0',
    'cache': 'no',
})

# 测试3：显式指定 mpegts demuxer
test("Test 3: demuxer=lavf, format=mpegts", avs2_file, {
    'demuxer': 'lavf',
    'demuxer-lavf-format': 'mpegts',
    'demuxer-lavf-probesize': '5000000',
    'demuxer-lavf-analyzeduration': '0',
    'cache': 'no',
})

print("\n=== All tests done ===")
