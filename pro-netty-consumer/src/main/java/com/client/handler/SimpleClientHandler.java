package com.client.handler;

import com.alibaba.fastjson2.JSONObject;
import com.client.core.DefaultFuture;
import com.client.param.Response;

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
		System.out.println("Receive result return from Server" + JSONObject.toJSONString(response));
		DefaultFuture.receive(response);
	//	ctx.channel().close();
	}



}
