package com.fitness.user.client

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.user.domain.AuthProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.ObjectProvider
import java.util.stream.Stream

class SocialTokenVerifiersTest {

    @Test
    fun `확인기가 없는 provider면 UNSUPPORTED_PROVIDER 예외를 던진다`() {
        // given
        val devVerifier = mockk<SocialTokenVerifier> { every { provider } returns AuthProvider.DEV }
        val beans = mockk<ObjectProvider<SocialTokenVerifier>> { every { orderedStream() } returns Stream.of(devVerifier) }
        val verifiers = SocialTokenVerifiers(beans)

        // when
        val exception = assertThrows<BusinessException> { verifiers.verify(AuthProvider.KAKAO, "kakao-token") }

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.UNSUPPORTED_PROVIDER)
    }
}
