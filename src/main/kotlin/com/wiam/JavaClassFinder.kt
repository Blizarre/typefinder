package com.wiam

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseProblemException
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

data class Type(val type_name: String)

class File(val identifier: String, stream: InputStream) {
    private val types = HashMap<Type, MutableSet<Int>>()

    // IrrecoverableError | TransientError

    val javaTypes: Map<Type, Set<Int>>
        get() = types

    fun add(type: Type, line: Int) = types.getOrPut(type) { HashSet() }.add(line)

    init {
        val cu = JavaParser.parse(stream)
        cu.accept(object : VoidVisitorAdapter<File>() {

            override fun visit(n: ClassOrInterfaceDeclaration, arg: File) {
                super.visit(n, arg)
                arg.add(Type(n.nameAsString), n.begin.get().line)
            }

            override fun visit(n: ClassOrInterfaceType, arg: File) {
                super.visit(n, arg)
                arg.add(Type(n.nameAsString), n.begin.get().line)
            }

        }, this)
    }

}

class Archive(val identifier: String, stream: InputStream) {
    private val files = ArrayList<File>()

    val javaFiles: List<File>
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
                        files.add(File(entry.name, noCloseIs))
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

class JavaClassProcessor(private val processQueue: Producer<Release>) : Runnable {
    override fun run() {
        do {
            val release = processQueue.get()
            try {
                val zipis = Archive(release.name, release.url.openStream())
                val files = zipis.javaFiles
                val nbTypes = files.map { it.javaTypes.size }.sum()
                log.info("Processed ${release.url} and found ${files.size} files ($nbTypes types)")
            } catch (ioException: IOException) {
                log.severe("Error processing ${release.url}: ${ioException.message}")
            }
        } while (true)
    }
}