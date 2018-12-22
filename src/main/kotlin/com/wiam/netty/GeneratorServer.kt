package com.wiam.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.net.InetSocketAddress

class GeneratorServer(private val port: Int) {

    fun start() {
        val serverBootstrap = ServerBootstrap()
        val group = NioEventLoopGroup()
        try {
            serverBootstrap.group(group)
                .channel(NioServerSocketChannel::class.java)
                .localAddress(InetSocketAddress("localhost", port))
                .childHandler(MyChannelInitializer())
            System.out.println("Listening on port $port")
            val channel = serverBootstrap.bind().sync().channel()
            channel.closeFuture().sync()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            group.shutdownGracefully().sync()
        }
    }
}