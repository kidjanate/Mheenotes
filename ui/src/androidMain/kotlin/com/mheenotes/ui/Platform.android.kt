package com.mheenotes.ui

import android.annotation.SuppressLint
import android.content.Context
import com.mheenotes.core.store.localOffsetMillis
import java.io.File
import java.util.TimeZone

actual object Platform {
    actual fun copyDirectory(from: String, to: String) {
        File(from).copyRecursively(File(to), overwrite = true)
    }

    actual fun copyFile(from: String, to: String) {
        val src = File(from)
        if (src.exists()) src.copyTo(File(to), overwrite = true)
    }
}

/** Android application context, for platform services that need it. */
@SuppressLint("StaticFieldLeak")
object AndroidContext {
    lateinit var app: Context
}

fun AppGraph.init(context: Context) {
    AndroidContext.app = context.applicationContext
    val tz = TimeZone.getDefault()
    localOffsetMillis = tz.getOffset(System.currentTimeMillis()).toLong()
    init(context.filesDir.resolve("mheenotes").absolutePath)
}
