package com.soco.chat;

import java.net.InetAddress;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;


import com.soco.log.Log;
import com.soco.msg.MsgBodyString;
import com.soco.msg.MsgData;
import com.soco.utility.Utility;


public class ChatServerHandler extends SimpleChannelInboundHandler<String> {

    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Once session is secured, send a greeting and register the channel to the global channel
        // list so the channel received the messages from others.
        
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(Future<Channel> future) throws Exception {
                        ctx.writeAndFlush(
                                "Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n");
                        ctx.writeAndFlush(
                                "Your session is protected by " +
                                        ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +
                                        " cipher suite.\n");

                        channels.add(ctx.channel());
                    }
        });
        
    }
    
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(Future<Channel> future) throws Exception {
                        Log.log("inactive of "+ctx.channel().remoteAddress());
                        //do something for the left user
                        
                    }
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // TODO Auto-generated method stub
        String str = msg;
        for (Channel c: channels) {
            try{
                try{
                    str = showMsgData(msg);
                }catch(Exception e){
                    Log.log(e.getMessage());
                }
                if (c != ctx.channel()) {
                    c.writeAndFlush("[" + ctx.channel().remoteAddress() + "] " + str + '\n');
                } else {
                    c.writeAndFlush("[you] " + str + '\n');
                }
                //
                Log.log(c.remoteAddress()+":"+str);
            }catch(Exception e){
                Log.log(e.getMessage());
            }
        }

        // Close the connection if the client has sent 'bye'.
        if ("bye".equals(str.toLowerCase())) {
            ctx.close();
        }
    }
    
 
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private String showMsgData(String msg){
        String ret = "";
        try {
            MsgData msgData = new MsgData(Utility.uncompressString(msg));
            
            if(msgData != null){
                Log.log("getSenderType:"+msgData.getHeader().getSenderType());
                Log.log("getSenderID:"+msgData.getHeader().getSenderID());
                Log.log("getReceiverType:"+msgData.getHeader().getReceiverType());
                Log.log("getReceiverID:"+msgData.getHeader().getReceiverID());
                Log.log("getBodyType:"+msgData.getHeader().getBodyType());
                Log.log("getMsgStr:"+((MsgBodyString)msgData.getBody()).getMsgStr());
                ret = ((MsgBodyString)msgData.getBody()).getMsgStr();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }
    
}
