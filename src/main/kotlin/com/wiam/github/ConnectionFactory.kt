package com.wiam.github

import java.net.HttpURLConnection
import java.net.URL

class InvalidURLException(message: String) : Exception(message)

class ConnectionFactory(private val token: String?) {

    fun connect(url: URL): HttpURLConnection {
        val connection = url.openConnection() as? HttpURLConnection
                ?: throw InvalidURLException("Expected a HTTP-like zipUrl, got ${url.protocol}")
        token?.let {
            connection.setRequestProperty("Authorization", "token $it")
        }
        connection.connect()
        return connection
    }
}
