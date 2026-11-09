package com.fitness.support

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/** 가짜 공개키(JWKS) 서버. 키 교체·장애·지연을 흉내 내고 요청 수를 센다. JDK 내장 HttpServer라 의존성이 없다 */
class FakeJwksServer : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    @Volatile var keys: List<RSAKey> = emptyList()

    @Volatile var failing = false // true면 500

    @Volatile var delay: Duration = Duration.ZERO
    val requestCount = AtomicInteger()

    init {
        server.createContext("/jwks") { exchange ->
            requestCount.incrementAndGet()
            Thread.sleep(delay.toMillis())
            val body = JWKSet(keys.map { it.toPublicJWK() }).toString().toByteArray()
            exchange.sendResponseHeaders(if (failing) 500 else 200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
    }

    val uri: String get() = "http://127.0.0.1:${server.address.port}/jwks"

    override fun close() = server.stop(0)
}
