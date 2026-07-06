package com.iptv.scanner.editor.pro

import android.util.Log
import com.chaquo.python.android.PyApplication
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.data.StringFormat

/**
 * 自定义 Application 类，继承 Chaquopy 的 [PyApplication]。
 *
 * 职责：
 * 1. 调用 super.onCreate() 完成 Chaquopy Python 初始化
 * 2. 初始化 ACRA（Java 异常崩溃捕获）
 * 3. 初始化 NativeCrashLogger（native SIGSEGV 崩溃捕获）
 *
 * 崩溃报告路径：
 * - Java 异常：getFilesDir()/ACRA/（ACRA 管理）
 * - Native 崩溃：getFilesDir()/crash-reports/（NativeCrashLogger 管理）
 */
class IptvApplication : PyApplication() {

    companion object {
        private const val TAG = "IptvApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化 ACRA（Java 异常崩溃捕获）
        try {
            val config = CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig::class.java)
                .withReportFormat(StringFormat.JSON)
            ACRA.init(this, config)
            Log.i(TAG, "ACRA initialized")
        } catch (e: Exception) {
            Log.e(TAG, "ACRA initialization failed", e)
        }

        // 初始化原生崩溃日志收集（SIGSEGV 等）
        try {
            NativeCrashLogger.init(this)
            Log.i(TAG, "NativeCrashLogger initialized")
        } catch (e: Exception) {
            Log.e(TAG, "NativeCrashLogger initialization failed", e)
        }
    }
}
