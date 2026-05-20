package com.example.hourlychime

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun `通知本文は時刻のみを表示する`() {
        assertEquals("00:00", NotificationHelper.buildTimeSignalText(0))
        assertEquals("01:00", NotificationHelper.buildTimeSignalText(1))
        assertEquals("12:00", NotificationHelper.buildTimeSignalText(12))
        assertEquals("23:00", NotificationHelper.buildTimeSignalText(23))
    }
}
