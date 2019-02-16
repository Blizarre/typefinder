package com.wiam.github.listreleases

import com.beust.klaxon.Klaxon
import com.wiam.github.GraphQLAPIConnection
import java.net.URL
import java.security.InvalidParameterException
import java.util.zip.GZIPInputStream

class RequestError(code: Int) : Exception("Request failed with code $code")
class InvalidResponse(invalidData: String) : Exception("Cannot parse data:\n$invalidData")

class JavaReposReleases(
        private val cnx: GraphQLAPIConnection = GraphQLAPIConnection(System.getProperty("github.token")
                ?: throw InvalidParameterException("Token not found"))
) {
    private val queryJavaRepos = ClassLoader.getSystemResourceAsStream("githubQuery.graphql").readAllBytes().toString(Charsets.UTF_8)

    var nextCursor: String? = null

    fun listReleases(): List<Release> {
        val rawResult = fetch(nextCursor)
        nextCursor = rawResult.pageInfo.endCursor

        return rawResult.edges.map {
            Release(
                    Repository(it.node.name, it.node.url),
                    it.node.releases.nodes.firstOrNull()?.tagName ?: it.node.defaultBranchRef.name
            )
        }
    }

    private fun fetch(cursor: String?): APIReleaseSearch {
        @Suppress("unused")
        val graphQLQuery = object {
            val query = queryJavaRepos
            val variables = object {
                val first = 100
                val after = cursor
            }
        }
        val connection = cnx.connect()
        connection.outputStream.write(Klaxon().toJsonString(graphQLQuery).toByteArray(Charsets.UTF_8))

        if (connection.responseCode != 200) {
            throw RequestError(connection.responseCode)
        }

        val jsonInput = Klaxon().parseJsonObject(GZIPInputStream(connection.inputStream).bufferedReader())
        return jsonInput.obj("data")?.obj("search")?.let {
            Klaxon().parseFromJsonObject<APIReleaseSearch>(it)
        } ?: throw InvalidResponse(jsonInput.toJsonString(true))
    }
}


// TODO: Make this class hierarchy more generic (generic edges/nodes maybe?)
// TODO Add test using the sample output in resources

data class APIReleaseSearch(val pageInfo: PageInfo, val repositoryCount: Int, val edges: List<Edge>)

data class Edge(val node: Node)

data class PageInfo(val startCursor: String, val endCursor: String)

data class Node(val name: String, val releases: GithubReleases, val defaultBranchRef: NameNode, val url: String)

class GithubReleases(val nodes: List<APIRelease>)

data class NameNode(val name: String) {
    override fun toString(): String = name
}

data class APIRelease(val url: String, val tagName: String)

data class Repository(val name: String, val url: String)

data class Release(val repository: Repository, val commitIdentifier: String) {
    val url = URL("${repository.url}/archive/$commitIdentifier.zip")
}
