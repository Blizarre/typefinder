package com.wiam.github

import java.net.HttpURLConnection
import java.net.URL

class GraphQLAPIConnection(private val token: String) {

    private val githubGraphQLApi = URL("https://api.github.com/graphql")

    fun connect(): HttpURLConnection {
        val connection = githubGraphQLApi.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.requestMethod = "POST"
        connection.addRequestProperty("Accept-Encoding", "gzip")
        connection.doOutput = true
        return connection
    }
}
