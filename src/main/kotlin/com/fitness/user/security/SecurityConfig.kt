package com.fitness.user.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/** 세션 없는 JWT 인증 (ADR 0004). 공개 경로만 나열하고 나머지는 모두 인증을 요구한다 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val entryPoint: ProblemDetailAuthenticationEntryPoint,
    private val accessDeniedHandler: ProblemDetailAccessDeniedHandler,
    private val publicPaths: PublicPathProperties,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // 쿠키 세션이 없어 CSRF 대상이 아니다
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() } // 우리 로그아웃은 /api/v1/auth/logout
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(*PUBLIC_PATHS).permitAll()
                if (publicPaths.extraPublicPaths.isNotEmpty()) {
                    it.requestMatchers(*publicPaths.extraPublicPaths.toTypedArray()).permitAll()
                }
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    companion object {
        private val PUBLIC_PATHS = arrayOf(
            "/api/v1/auth/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error", // 서블릿 오류 처리가 원래 응답을 401로 덮지 않게
        )
    }
}
