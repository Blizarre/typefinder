package com.wiam

import java.net.HttpURLConnection
import java.net.URL

class InvalidURLException(message: String) : Exception(message)

class ConnectionFactory {
    fun connect(url: URL): HttpURLConnection {
        val connection = url.openConnection() as? HttpURLConnection
            ?: throw InvalidURLException("Expected a HTTP-like url, got ${url.protocol}")
        connection.connect()
        return connection
    }
}
