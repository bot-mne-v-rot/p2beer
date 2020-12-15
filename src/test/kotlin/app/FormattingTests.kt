package app

import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals

class FormattingTests {
    @Test
    fun `check formatting`() {
        val sdf = SimpleDateFormat("dd MMMM, HH:mm", Locale.ENGLISH)

        assertEquals("15 December, 21:50",
        sdf.format(Date(1608058249247)))
    }
}