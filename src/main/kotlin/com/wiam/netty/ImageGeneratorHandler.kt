package com.wiam.netty

import com.beust.klaxon.Klaxon
import com.wiam.combinator.multiply
import com.wiam.generator.CheckerBoard
import com.wiam.generator.Fractal
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.CharsetUtil
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.net.URI
import java.security.InvalidParameterException
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO


class ImageGeneratorHandler : ChannelInboundHandlerAdapter() {
    private val generators = mapOf(
        "Checker" to CheckerBoard(200, 200),
        "Fractal" to Fractal(200, 200)
    )

    private val index = createHTMLDocument().div {
        h1 {
            +"It works!"
        }
        p {
            ul {
                li {
                    img {
                        src = "/generator/Fractal/100"
                    }
                }
                li {
                    img {
                        src = "/generator/Checker/50"
                    }
                }
            }
        }
    }.serialize()

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            Unpooled.copiedBuffer(
                "Error when processing the request: ${cause?.message}", Charsets.UTF_8
            )
        )
        ctx?.channel()?.writeAndFlush(response)?.addListener(ChannelFutureListener.CLOSE)
        cause?.printStackTrace()
    }

    class Parameters(path: String) {
        val type: String
        val seed: Int

        init {
            val params = path
                .split("/")
                .filter(String::isNotEmpty)
            if (params.size != 3) {
                throw InvalidParameterException("Invalid URL")
            }
            type = params[1]
            seed = params[2].toIntOrNull() ?: throw InvalidParameterException("Invalid seed in URL")
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val request = msg as HttpRequest
        System.out.println("Request ${request.method()} ${request.uri()}")
        val uri = URI.create(request.uri())
        when {
            uri.path == "/" -> {
                val response = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.ACCEPTED,
                    Unpooled.copiedBuffer(
                        index,
                        CharsetUtil.UTF_8
                    )
                )
                response.headers().add("Content-Type", "text/html; charset=utf-8")
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
            uri.path.startsWith("/avatar/") -> {
                val seed = uri.path.split("/")[2].toInt()
                System.out.println("Seed $seed")
                val response = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.ACCEPTED,
                    Unpooled.copiedBuffer(
                        serialize(
                            multiply(
                                generators["Fractal"]!!.getImage(seed.and(0xFF)),
                                generators["Checker"]!!.getImage(seed.ushr(8).and(0xFF))
                            )
                        )
                    )
                )
                response.headers().add("Content-Type", "image/png")
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
            uri.path == "/generator" -> {
                val data = Klaxon().toJsonString(generators.keys)
                val response = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.ACCEPTED,
                    Unpooled.copiedBuffer(Unpooled.copiedBuffer(data, Charsets.UTF_8))
                )
                response.headers().add("Content-Type", "application/json; charset=utf-8")
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
            uri.path.startsWith("/matrix/avatar") -> {
                val matrix = createHTMLDocument().div {
                    h1 {
                        +"It works!"
                    }
                    p {
                        (0 until 255).map {
                            a {
                                href = "/avatar/${Random().nextInt()}"
                                img {
                                    src = "/avatar/${Random().nextInt()}"
                                    style = "margin-left:3px"
                                }
                            }
                        }
                    }
                }.serialize()

                val response = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.ACCEPTED,
                    Unpooled.copiedBuffer(Unpooled.copiedBuffer(matrix, Charsets.UTF_8))
                )
                response.headers().add("Content-Type", "text/html; charset=utf-8")
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
            uri.path.startsWith("/matrix/") -> {
                val type = uri.path.split("/")[2]
                val matrix = createHTMLDocument().div {
                    h1 {
                        +"It works!"
                    }
                    p {
                        (0 until 255).map {
                            img {
                                src = "/generator/$type/$it"
                            }
                        }
                    }
                }.serialize()

                val response = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.ACCEPTED,
                    Unpooled.copiedBuffer(Unpooled.copiedBuffer(matrix, Charsets.UTF_8))
                )
                response.headers().add("Content-Type", "text/html; charset=utf-8")
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
            uri.path.startsWith("/generator/") -> {
                val instructions = Parameters(uri.path)
                val response = DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.ACCEPTED,
                    Unpooled.copiedBuffer(getImageData(instructions))
                )
                response.headers().add("Content-Type", "image/png")
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
            }
        }
    }


    private fun serialize(image: IntArray): ByteBuf {
        val begin = Instant.now()
        val buff = Unpooled.buffer()
        val imageByte = ByteArray(image.size)

        image.forEachIndexed { i, v -> imageByte[i] = v.coerceIn(0, 255).toByte() }

        val buffer = DataBufferByte(imageByte, 200 * 200)

        val cs = ColorSpace.getInstance(ColorSpace.CS_GRAY)
        val nBits = intArrayOf(8)
        val cm = ComponentColorModel(
            cs, nBits, false, true,
            Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        )
        val sm = cm.createCompatibleSampleModel(200, 200)

        val raster = Raster.createWritableRaster(sm, buffer, null)

        val img = BufferedImage(cm, raster, true, null)

        if (!ImageIO.write(img, "PNG", ByteBufOutputStream(buff))) {
            throw InvalidParameterException("Could not write the image")
        }
        val elapsed = Duration.between(begin, Instant.now())
        System.out.println("Serialization time: ${elapsed.toMillis()}")
        return buff
    }

    private fun getImageData(p: Parameters): ByteBuf {
        val generator = generators
            .getOrElse(p.type) {
                throw InvalidParameterException("Unknown image type")
            }
        return serialize(generator.getImage(p.seed))
    }
}

