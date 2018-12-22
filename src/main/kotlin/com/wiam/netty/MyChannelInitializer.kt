package com.wiam.netty

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec

class MyChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?) {
        ch?.pipeline()?.addLast(HttpServerCodec())
        ch?.pipeline()?.addLast(HttpObjectAggregator(20000))
        ch?.pipeline()?.addLast(ImageGeneratorHandler())
    }
}