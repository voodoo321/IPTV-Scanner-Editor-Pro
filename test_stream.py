import urllib.request
import socket

url = 'http://192.168.50.18:20231/rtp/239.21.1.164:5002'
req = urllib.request.Request(url)
req.add_header('User-Agent', 'mpv')
try:
    resp = urllib.request.urlopen(req, timeout=5)
    print(f'Status: {resp.status}')
    data = resp.read(4096)
    print(f'Got {len(data)} bytes')
    print(data[:100])
except Exception as e:
    print(f'ERROR: {e}')
