package com.wiam

import com.wiam.netty.GeneratorServer


fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Error: Port number expected")
        System.exit(1)
    }
    val port: Int
    try {
        port = args[0].toInt()
    } catch (e: NumberFormatException) {
        System.err.println("Error: Invalid port number")
        System.exit(2)
        return
    }

    val server = GeneratorServer(port)
    server.start()
}

