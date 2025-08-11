package com.netty.handler;

import com.alibaba.fastjson2.JSONObject;
import com.netty.client.DefaultFuture;
import com.netty.util.Response;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

public class SimpleClientHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		if ("ping".equals(msg.toString())) {
			ctx.channel().writeAndFlush("ping\r\n");
			return;
		}
		
//		ctx.channel().attr(AttributeKey.valueOf("ssss")).set(msg);
		Response response = JSONObject.parseObject(msg.toString(), Response.class);
		DefaultFuture.receive(response);
	//	ctx.channel().close();
	}



}
