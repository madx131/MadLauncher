package com.madlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ActionBoxActivityTest {
    @Test
    fun openActionDoesNotUseHomeLaunch() {
        assertEquals("com.madlauncher.action.OPEN_ACTION_BOX", ActionBoxActivity.ACTION_OPEN)
        assertFalse(ActionBoxActivity.ACTION_OPEN.contains("MAIN"))
    }
}
