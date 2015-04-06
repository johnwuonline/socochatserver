package com.soco.chat;

import com.soco.XML.XmlConfig;
import com.soco.log.Log;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class ChatServer {
    private static String config_xml_file = "config.xml";
    static int PORT = Integer.parseInt(System.getProperty("port", "28992"));

    public static void main(String[] args) throws Exception {
        Log.DEBUG_ON = true;
        Log.log("To start heart beat server...");
        Log.log(System.getProperty("user.dir"));
        String configXml = ChatServer.config_xml_file;
        if(args.length > 0){
            configXml = args[0];
        }
        Log.log("Load " + configXml + " config file...");
        XmlConfig config = new XmlConfig(configXml);
        config.setServerName("HeartBeatServer").setConnectionNode("TCPConnection").parse();
        Log.log("Port is "+config.getPort());
        if(0 != config.getPort()){
            PORT = config.getPort();
        }
        ////------->
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChatServerInitializer(sslCtx));

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
