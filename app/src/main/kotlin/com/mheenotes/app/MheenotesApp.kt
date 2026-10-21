package com.mheenotes.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.dev.Fixtures
import com.mheenotes.ui.init

class MheenotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fixtures.enabled = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        AppGraph.init(this)
    }
}
