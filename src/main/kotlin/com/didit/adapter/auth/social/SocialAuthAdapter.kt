package com.didit.adapter.auth.social

import com.didit.adapter.auth.social.oidc.AppleOidcVerifier
import com.didit.adapter.auth.social.oidc.GoogleOidcVerifier
import com.didit.adapter.auth.social.oidc.KakaoOidcVerifier
import com.didit.application.auth.required.social.SocialAuthPort
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import com.didit.domain.auth.enums.SocialProvider
import com.didit.domain.auth.model.SocialUserInfo
import org.springframework.stereotype.Component

@Component
class SocialAuthAdapter(
    private val googleOidcVerifier: GoogleOidcVerifier,
    private val appleOidcVerifier: AppleOidcVerifier,
    private val kakaoOidcVerifier: KakaoOidcVerifier,
) : SocialAuthPort {
    override fun verifyIdToken(
        provider: SocialProvider,
        idToken: String,
    ): SocialUserInfo =
        when (provider) {
            SocialProvider.GOOGLE -> {
                try {
                    val googleIdToken = googleOidcVerifier.verify(idToken)

                    SocialUserInfo(
                        provider = provider,
                        socialId = googleIdToken.subject,
                        email = googleIdToken.email,
                    )
                } catch (e: Exception) {
                    throw BusinessException(ErrorCode.INVALID_ID_TOKEN)
                }
            }

            SocialProvider.APPLE -> {
                try {
                    val appleIdToken = appleOidcVerifier.verify(idToken)
                    SocialUserInfo(
                        provider = provider,
                        socialId = appleIdToken.subject,
                        email = appleIdToken.email,
                    )
                } catch (e: Exception) {
                    throw BusinessException(ErrorCode.INVALID_ID_TOKEN)
                }
            }

            SocialProvider.KAKAO -> {
                try {
                    val payload = kakaoOidcVerifier.verify(idToken)
                    SocialUserInfo(
                        provider = provider,
                        socialId = payload.subject,
                        email = payload.email,
                    )
                } catch (e: Exception) {
                    throw BusinessException(ErrorCode.INVALID_ID_TOKEN)
                }
            }
        }
}
