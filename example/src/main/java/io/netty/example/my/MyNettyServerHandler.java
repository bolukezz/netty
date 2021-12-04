package io.netty.example.my;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * 说明
 * 1.自定义一个handler 需要继承netty规定好的某个HandlerAdapter(规范）
 * @author jiangl
 * @version 1.0
 * @date 2021/5/23 10:41
 */
public class MyNettyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 读取数据实际（可以读取客户端发送的消息）
     * @param ctx 上下文对象，含有管道pipeline ，通道channel
     * @param msg 客户端发送的数据，默认Object类型
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("服务器读取线程 "+ Thread.currentThread().getName());
        System.out.println("server ctx="+ctx);
        //将msg 转成一个 ByteBuf
        ByteBuf buf = (ByteBuf) msg;
        System.out.println("客户端发送消息是："+buf.toString(CharsetUtil.UTF_8));
        System.out.println("客户端的地址是："+ctx.channel().remoteAddress());
    }

    /**
     * 数据读取完毕
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        /**
         * 将数据写入到缓冲并刷新
         * 一般需要对发送的数据进行编码
         */
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello，客户端~",CharsetUtil.UTF_8));
    }

    /**
     * 处理异常，关闭通道
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
