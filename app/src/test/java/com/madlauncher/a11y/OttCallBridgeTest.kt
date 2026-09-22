package com.madlauncher.a11y

import android.content.Intent
import org.junit.Test

class OttCallBridgeTest {
    @Test
    fun stashRelaunch_doesNotCrash() {
        OttCallBridge.stashRelaunch(Intent(Intent.ACTION_VIEW))
        OttCallBridge.clear()
    }
}
