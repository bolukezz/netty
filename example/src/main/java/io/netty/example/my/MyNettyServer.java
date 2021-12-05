package io.netty.example.my;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author jiangl
 * @version 1.0
 * @date 2021/5/22 23:39
 */
public class MyNettyServer {

    public static void test(){
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,bossGroup);
    }

    public void test2(){
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workGroup);

    }

    public  void test4(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workGroup);
    }

    public static void main(String[] args) throws InterruptedException {
        //创建BossGroup workerGroup
        //说明
        //1.创建两个线程组bossGroup workerGroup
        //2.bossGroup 只是处理连接请求，真正的和客户端业务处理，会交给workerGroup完成
        //3.两个都是无限循环
        //4.bossGroup 和 workerGroup含有的子线程（NioEventLoop)的个数
        //默认实际 cpu 核数 * 2
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);

        try {
            //创建服务器端的启动对象，配置参数
            ServerBootstrap bootstrap = new ServerBootstrap();

            /**
             * 这里可以发现我们的handler的添加也有2个，一个是通过handler方法设置的handler，一个是通过childHandler方法设置的childHandler，
             * 这里其实可以知道，Handler与accept过程有关，即Handler负责处理客户端新连接接入的请求，而childHandler就是负责和客户端的IO交互，
             * 可以通过ServerBootstrap的init方法来看如何添加Handler
             *
             * handler和childHandler的区别
             *1、在服务端NioServerSocketChannel对象的Pipeline中添加了Handler对象和ServerBootstrapAcceptor对象
             * 2、当有新的客户端连接请求时，会调用ServerBootstrapAcceptor的channelRead()方法创建此连接的NioSocketChannel对象
             * 并将childHandler添加到NioSocketChannel对应的Pipeline中，而且将此Channel绑定到workerGroup中的某个EventLoop中
             * 3、Handler对象只在accept()阻塞阶段起作用，它主要处理客户端发送过来的连接请求
             * 4、childHandler在客户端建立连接后起作用，它负责客户端连接的IO交互。
             */
            //使用链式编程来进行设置
            bootstrap.group(bossGroup,workerGroup)//设置两个线程组
                    .channel(NioServerSocketChannel.class)//使用NioSocketChannel 作为服务器的通道实现
                    .option(ChannelOption.SO_BACKLOG,128)//设置队列得到连接个数
                    .childOption(ChannelOption.SO_KEEPALIVE,true)//设置保持活动连接状态
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        //创建一个通道测试对象
                        /**
                         * 给pipline 设置处理器
                         * @param ch
                         * @throws Exception
                         */
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new MyNettyServerHandler());
                        }
                    });//给workerGroup的EventLoop对应的管道设置处理器
            System.out.println("........服务器 is ready");

            //绑定一个端口并且同步，生成了一个ChannelFuture对象
            //调用bind方法来监听一个本地端口
            ChannelFuture sync = bootstrap.bind(6668).sync();

            //对关闭通道进行监听
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //优雅的关闭
            bossGroup.shutdownGracefully();
        }
    }
}


