package com.weathersnap

import com.weathersnap.domain.model.WeatherCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeTest {

    @Test fun `code 0 maps to Clear sky`() {
        assertEquals("Clear sky", WeatherCode.describe(0))
    }

    @Test fun `code 95 maps to Thunderstorm`() {
        assertEquals("Thunderstorm", WeatherCode.describe(95))
    }

    @Test fun `null maps to Unknown`() {
        assertEquals("Unknown", WeatherCode.describe(null))
    }

    @Test fun `unmapped code maps to Unknown`() {
        assertEquals("Unknown", WeatherCode.describe(999))
    }
}
