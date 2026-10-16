package id.project.df.dble.feature.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartLabelTopTest {
    @Test
    fun `label dipusatkan pada garis grid`() {
        assertEquals(40f, chartLabelTop(y = 50f, labelHeight = 20f, canvasHeight = 100f), 0f)
    }

    @Test
    fun `label di tepi atas digeser masuk`() {
        assertEquals(0f, chartLabelTop(y = 0f, labelHeight = 20f, canvasHeight = 100f), 0f)
    }

    @Test
    fun `label di tepi bawah digeser masuk`() {
        assertEquals(80f, chartLabelTop(y = 100f, labelHeight = 20f, canvasHeight = 100f), 0f)
    }

    @Test
    fun `canvas lebih pendek dari label tidak crash`() {
        assertEquals(0f, chartLabelTop(y = 5f, labelHeight = 20f, canvasHeight = 10f), 0f)
        assertEquals(0f, chartLabelTop(y = 0f, labelHeight = 20f, canvasHeight = 0f), 0f)
    }
}
