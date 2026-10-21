package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconStyleContractTest {
    @Test
    fun parsesAMaskAndRejectsBadPoints() {
        val ok = IconStyleContract.parse(
            """
            {"format":1,"id":"my-dogs","name":"My dogs","mask":{"points":[[0.5,0.05],[0.95,0.95],[0.05,0.95]]}}
            """.trimIndent(),
        )
        assertNotNull(ok)
        assertEquals("my-dogs", ok!!.id)
        assertEquals(3, ok.mask!!.points.size)
        assertEquals(0.66f, ok.mask.logo.scale, 0.001f)
        assertTrue(ok.icons.isEmpty())

        assertNull(
            IconStyleContract.parse(
                """
                {"format":1,"id":"my-dogs","name":"My dogs","mask":{"points":[[1.2,0.1],[0.1,0.9],[0.2,0.2]]}}
                """.trimIndent(),
            ),
        )
        assertNull(IconStyleContract.parse("""{"format":1,"id":"Bad Id","name":"Nope","mask":{"points":[[0,0],[1,0],[0,1]]}}"""))
        assertNull(IconStyleContract.parse("""{"format":2,"id":"ok","name":"Nope"}"""))
        assertNull(IconStyleContract.parse("""{"format":1,"id":"ok","name":"Empty"}"""))
    }

    @Test
    fun rejectsUnsafeIconPaths() {
        assertNull(
            IconStyleContract.parse(
                """
                {"format":1,"id":"pack","name":"Pack","icons":{"com.example":"../secret.png"}}
                """.trimIndent(),
            ),
        )
        val ok = IconStyleContract.parse(
            """
            {"format":1,"id":"pack","name":"Pack","icons":{"com.whatsapp/.Main":"icons/whatsapp.webp"}}
            """.trimIndent(),
        )
        assertEquals("icons/whatsapp.webp", ok!!.icons["com.whatsapp/.Main"])
        assertTrue(IconStyleContract.isSafeRelativePath("icons/whatsapp.webp"))
        assertTrue(!IconStyleContract.isSafeRelativePath("icons/../x.png"))
    }
}
