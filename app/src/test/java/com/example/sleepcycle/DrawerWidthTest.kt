package com.example.sleepcycle

import com.example.sleepcycle.ui.drawerWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerWidthTest {

    @Test
    fun narrowScreensTake80PercentLeavingAPeekOfContent() {
        assertEquals(256f, drawerWidth(320).value, 0.01f)
        assertEquals(288f, drawerWidth(360).value, 0.01f)
        assertEquals(328.8f, drawerWidth(411).value, 0.01f)
    }

    @Test
    fun wideScreensCapAtTheMaterialDefault360dp() {
        assertEquals(360f, drawerWidth(480).value, 0.01f)
        assertEquals(360f, drawerWidth(1024).value, 0.01f)
    }
}
