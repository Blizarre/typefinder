package com.wiam

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JavaClassFinderTest {
    @Test
    fun testProcessFile() {
        val fileResourceStream = this.javaClass.classLoader.getResourceAsStream("helloworld.java")!!
        val results = File("test", fileResourceStream)
        Assertions.assertEquals(
            mapOf(
                Type("HelloWorld") to setOf(5),
                Type("String") to setOf(14),
                Type("Bonobo") to setOf(7),
                Type("Foo") to setOf(9),
                Type("TestType") to setOf(9, 11),
                Type("Bar") to setOf(10, 10)
            ),
            results.javaTypes
        )
    }

    @Test
    fun testProcessArchive() {
        val archiveResourceStream = this.javaClass.classLoader.getResourceAsStream("testArchive.zip")!!
        val results = Archive("none", archiveResourceStream)
        val expected = mapOf(
            "File 1.java" to mapOf(
                Type("HelloWorld") to setOf(5),
                Type("Bonobo") to setOf(7),
                Type("String") to setOf(9)
            ),
            "src/File2.java" to mapOf(
                Type("HelloWorld") to setOf(5),
                Type("TestType") to setOf(7),
                Type("Foo") to setOf(7)
            )
        )

        Assertions.assertEquals(expected.size, results.javaFiles.size)

        results.javaFiles.forEach {
            Assertions.assertTrue(
                expected.containsKey(it.identifier),
                "No file ${it.identifier} in ${expected.keys.joinToString(", ")}"
            )
            Assertions.assertEquals(expected[it.identifier], it.javaTypes)
        }
    }
}