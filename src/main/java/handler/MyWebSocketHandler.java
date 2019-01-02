package handler;

import config.NettyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Date;

/**
 * 接受/处理/响应客户端webSocket请求的核心业务处理类
 *
 * Created by Yuk on 2019/1/1.
 */
public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object>{

    private WebSocketServerHandshaker handshaker;
    private static final String WEB_SOCKET_URL = "ws//localhost:9998/echo";

    /**
     * 客户端与服务端创建连接时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        System.out.println("客户端与服务端连接开启。。。");
    }

    /**
     * 客户端与服务端断开连接时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.remove(ctx.channel());
        System.out.println("客户端与服务端断开连接。。。");
    }

    /**
     * 服务端接收客户端发送的数据结束后调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 异常调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端处理客户端webSocket请求的核心方法
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 处理客户端向服务端发起ttp握手请求的业务
        if(msg instanceof FullHttpRequest){
            handlerHttpRequest(ctx,(FullHttpRequest) msg);
        }
        // 处理webSocket连接业务
        else if(msg instanceof WebSocketFrame)
        {
            handlerWebSocketFrame(ctx,(WebSocketFrame)msg);
        }
    }

    /**
     * 处理客户端向服务端发起的webSocket连接业务
     * @param ctx
     * @param frame
     */
    private void handlerWebSocketFrame(ChannelHandlerContext ctx,WebSocketFrame frame) {
        // 判断是否是关闭webSocket的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        }

        // 判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }

        // 判断是否是二进制消息，是则抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("目前不支持二进制消息");
            throw new RuntimeException("【" + this.getClass().getName() + "】不支持二进制消息");
        }

        // 返回应答
        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("服务端收到客户端的消息===" + request);

        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString()
                                                        +ctx.channel()
                                                        .id()
                                                        +"===>>>"
                                                        +request);
        // 群发，服务向每个连接上来的客户群发消息
        NettyConfig.group.writeAndFlush(tws);
    }
      /**
     * 处理客户端向服务端发起的http握手请求的业务
     * @param ctx
     * @param req
     */
    private void handlerHttpRequest(ChannelHandlerContext ctx,FullHttpRequest req){
        // 请求失败或者不是webSocket请求
        if(!req.getDecoderResult().isSuccess() || !"websocket".equals(req.headers().get("Upgrade"))){
            sendHttpResponse(ctx,req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL,null,false);
        handshaker = wsFactory.newHandshaker(req);
        if(handshaker == null){
            // 将错误信息返回
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else{
            handshaker.handshake(ctx.channel(),req);
        }

    }

    /**
     * 服务端向客户端相应消息
     * @param ctx
     * @param req
     * @param resp
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse resp){

        if(resp.getStatus().code() != 200){
            ByteBuf buf = Unpooled.copiedBuffer(resp.getStatus().toString(), CharsetUtil.UTF_8);
            // 写入resp
            resp.content().writeBytes(buf);
            buf.release();
        }

        // 发送数据
        ChannelFuture future = ctx.channel().writeAndFlush(resp);
        if(resp.getStatus().code() != 200){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
