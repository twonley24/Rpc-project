package com.netty.handler;

import com.alibaba.fastjson2.JSONObject;
import com.netty.handler.param.ServerRequest;
import com.netty.util.Response;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class SimpleServerHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		ctx.channel().writeAndFlush("is ok\r\n");
		
		ServerRequest request =JSONObject.parseObject(msg.toString(),ServerRequest.class);
		Response resp = new Response();
		resp.setId(request.getId());
		resp.setResult("is ok");
		ctx.channel().writeAndFlush(JSONObject.toJSONString(resp));
		ctx.channel().writeAndFlush("\r\n");
	//	ctx.channel().close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	    if (evt instanceof IdleStateEvent) {
	        IdleStateEvent event = (IdleStateEvent) evt;
	        if (event.state().equals(IdleState.READER_IDLE)) {
	            System.out.println("Read Idle===");
	            ctx.channel().close();
	        } else if (event.state().equals(IdleState.WRITER_IDLE)) {
	            System.out.println("Write Idle===");
	        } else if (event.state().equals(IdleState.ALL_IDLE)) {
	            System.out.println("All Idle");
	            ctx.channel().writeAndFlush("ping\r\n");
	        }
	    }
	}
}
