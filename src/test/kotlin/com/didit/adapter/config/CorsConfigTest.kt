package com.didit.adapter.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class CorsConfigTest {
    @Test
    fun `쉼표 뒤 공백이 있는 origin도 허용 목록에 정상 등록한다`() {
        val source = CorsConfig("https://dev-app.didit.io.kr, http://localhost:3000").corsConfigurationSource()

        val configuration = source.getCorsConfiguration(MockHttpServletRequest())

        assertThat(configuration?.allowedOrigins)
            .containsExactly("https://dev-app.didit.io.kr", "http://localhost:3000")
    }
}
