package com.wiam

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseProblemException
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.wiam.persistence.Types
import com.wiam.stats.Statistics
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

data class Type(val type_name: String)

class ReleaseFile(val path: String, stream: InputStream) {
    private val types = HashMap<Type, MutableSet<Int>>()

    // TODO: add 2 kind of errors (system-wide?)
    // IrrecoverableError | TransientError

    val javaTypes: Map<Type, Set<Int>>
        get() = types

    fun add(type: Type, line: Int) = types.getOrPut(type) { HashSet() }.add(line)

    init {
        val cu = JavaParser.parse(stream)
        cu.accept(object : VoidVisitorAdapter<ReleaseFile>() {

            override fun visit(n: ClassOrInterfaceDeclaration, arg: ReleaseFile) {
                super.visit(n, arg)
                arg.add(Type(n.nameAsString), n.begin.get().line)
            }

            override fun visit(n: ClassOrInterfaceType, arg: ReleaseFile) {
                super.visit(n, arg)
                arg.add(Type(n.nameAsString), n.begin.get().line)
            }

        }, this)
    }

}

class ReleaseArchive(private val identifier: String, stream: InputStream) {
    private val files = ArrayList<ReleaseFile>()

    val javaFiles: List<ReleaseFile>
        get() = files

    init {
        val zipIs = ZipInputStream(stream)
        val noCloseIs = object : FilterInputStream(zipIs) {
            override fun close() {
                // no-op, we will close the stream manually once everything is done
            }
        }
        zipIs.use {
            var entry = zipIs.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".java")) {
                    log.fine("Found java file: ${entry.name}")
                    try {
                        val name = entry.name.removeRange(0..entry.name.indexOfFirst { it == '/' })
                        files.add(ReleaseFile(name, noCloseIs))
                    } catch (e: ParseProblemException) {
                        val message = e.problems.take(3).joinToString(", ") { it.verboseMessage }
                        log.warning("Could not parse file ${entry.name} in $identifier: $message")
                    }
                }
                entry = zipIs.nextEntry
            }
        }
    }
}

class JavaClassProcessor(private val processQueue: Producer<Release>, private val database: Types, private val stats: Statistics) : Runnable {
    override fun run() {
        do {
            val release = processQueue.get()
            try {
                stats.add("java.archive.total", 1)
                val zipis = ReleaseArchive(release.name, release.zipUrl.openStream())
                val files = zipis.javaFiles
                stats.add("java.files.total", files.size)

                val nbTypes = files.map { it.javaTypes.size }.sum()
                transaction {
                    files.forEach { file ->
                        file.javaTypes.forEach {
                            val (type, lines) = it
                            lines.forEach { lineInFile ->
                                stats.add("java.types.total", 1)
                                database.insert(release.repository.full_name, type.type_name, release.htmlUrl(file.path), lineInFile)
                            }
                        }
                    }
                }
                log.info("Processed ${release.zipUrl} and found ${files.size} files ($nbTypes types)")
            } catch (ioException: IOException) {
                stats.add("java.process.error", 1)
                log.severe("Error processing ${release.zipUrl}: ${ioException.message}")
            }
        } while (true)
    }
}