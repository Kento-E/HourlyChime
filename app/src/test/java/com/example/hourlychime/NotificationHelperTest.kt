package com.example.hourlychime

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun `通知本文は時刻のみを表示する`() {
        assertEquals("12:00", NotificationHelper.buildTimeSignalText(12))
    }
}
