package handler;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import future.ResultFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import medium.Medium;
import model.Response;
import model.ServerRequest;


/**
 * ServerHandler 是 Netty 服务端的核心处理器，用于接收客户端请求并返回响应。
 *
 * 工作流程：
 * 1. 当客户端有消息发送过来时，会触发 channelRead() 方法；
 * 2. 将接收到的 JSON 字符串反序列化为 ServerRequest 对象；
 * 3. 交给 Medium（中介者模式）进行处理，Medium 会根据请求的服务名/方法名找到对应的 Bean 和方法并执行；
 * 4. 将执行结果封装为 Response，并序列化为 JSON，通过 ctx.channel().writeAndFlush() 返回给客户端。
 *
 * 线程模型：
 * - 为了避免耗时业务阻塞 Netty 的 I/O 线程，这里使用了一个固定大小的线程池 (Executors.newFixedThreadPool(10))；
 * - 每个请求会被提交到线程池异步执行，保证 I/O 线程能专注于网络事件处理。
 *
 * 关键点：
 * - 继承自 ChannelInboundHandlerAdapter，重写 channelRead() 处理入站消息；
 * - 使用 JSONObject 将字符串和对象进行序列化/反序列化；
 * - 在返回数据时手动加上 "\r\n"，因为前面 pipeline 使用了 DelimiterBasedFrameDecoder，以换行作为消息分隔符。
 *
 * 使用场景：
 * - 服务端接收 RPC 客户端请求；
 * - 根据请求动态调用服务端 Controller/Service 方法；
 * - 将调用结果返回给客户端，实现远程调用。
 */


public class ServerHandler extends ChannelInboundHandlerAdapter  {
	private static final Executor exec = Executors.newFixedThreadPool(10);
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("服务器Handler:"+msg.toString());
//		ServerRequest serverRequest = JSONObject.parseObject(msg.toString(), ServerRequest.class);
//		System.out.println(serverRequest.getCommand());
		exec.execute(new Runnable() {
			
			@Override
			public void run() {
				ServerRequest serverRequest = JSONObject.parseObject(msg.toString(), ServerRequest.class);
				System.out.println(serverRequest.getCommand());
				Medium medium = Medium.newInstance();//生成中介者模式
				
				Response response = medium.process(serverRequest);
				
				//向客户端发送Resonse
				ctx.channel().writeAndFlush(JSONObject.toJSONString(response)+"\r\n");
			}
		});
//		Medium medium = Medium.newInstance();//生成中介者模式
//		
//		Response response = medium.process(serverRequest);
//		
//		//向客户端发送Resonse
//		ctx.channel().writeAndFlush(JSONObject.toJSONString(response)+"\r\n");
		
	}

//	@Override
//	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//		
//		if(evt instanceof IdleStateEvent){
//			IdleStateEvent event = (IdleStateEvent)evt;
//			
//			if(event.state().equals(IdleState.READER_IDLE)){
//				System.out.println("读空闲");
//			}
//			if(event.state().equals(IdleState.WRITER_IDLE)){
//				System.out.println("写空闲");
//			}
//			if(event.state().equals(IdleState.ALL_IDLE)){
//				System.out.println("读写空闲");
//				ctx.channel().writeAndFlush("ping\r\n");
//			}
//		}
//	}
	
	
	
}
