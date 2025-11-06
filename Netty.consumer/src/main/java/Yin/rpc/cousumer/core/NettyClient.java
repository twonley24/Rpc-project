
package Yin.rpc.cousumer.core;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;

import com.alibaba.fastjson.JSONObject;

import Yin.rpc.cousumer.constans.Constans;
import Yin.rpc.cousumer.handler.SimpleClientHandler;
import Yin.rpc.cousumer.param.ClientRequest;
import Yin.rpc.cousumer.param.Response;
import Yin.rpc.cousumer.zk.ServerWatcher;
import Yin.rpc.cousumer.zk.ZooKeeperFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * NettyClient
 * -----------------------------------------------
 * 🔹 作用：
 * 这是 RPC 客户端（Consumer）通信层的核心类，
 * 负责建立与服务端（Provider）的 Netty 长连接，
 * 管理通信管道（Channel），并通过 Zookeeper 实现服务发现与动态连接。
 *
 * 🔹 核心功能：
 * 1. 在启动时通过 Zookeeper 获取所有可用的 Provider 节点（IP + 端口）；
 * 2. 为每个 Provider 创建 Netty 连接（Channel）并交给 ChannelManager 管理；
 * 3. 监听 Zookeeper 节点变化（ServerWatcher），实现服务动态上下线；
 * 4. 提供统一的 send() 方法，用于发送 ClientRequest 请求并接收异步结果。
 *
 * 🔹 背景：
 * - 基于 Netty NIO 实现高性能网络通信；
 * - 使用 Zookeeper 做服务发现和连接监听；
 * - 与上层的 InvokeProxy 动态代理协作，实现透明的远程方法调用。
 *
 * -----------------------------------------------
 * ⚙️ 调用链：
 * InvokeProxy → NettyClient.send(request)
 *             → ChannelManager.selectChannel()
 *             → Netty 写出 JSON 请求 → SimpleClientHandler 接收响应
 *             → ResultFuture.get(timeout) 等待返回
 *
 * @author Taoge
 */

public class NettyClient {
//	public static Set<String> realServerPath = new HashSet<String>();//去重and去序列号
	public static final Bootstrap b = new Bootstrap();


	private static ChannelFuture f = null;
	
	static{
		String host = "localhost";
		int port = 8080;
		
		EventLoopGroup work = new NioEventLoopGroup();
		try {
		b.group(work)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							
							ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
							ch.pipeline().addLast(new StringDecoder());//字符串解码器
							ch.pipeline().addLast(new StringEncoder());//字符串编码器
							ch.pipeline().addLast(new SimpleClientHandler());//业务逻辑处理处
						}
			});
				
				CuratorFramework client = ZooKeeperFactory.getClient();
			
				List<String> serverPath = client.getChildren().forPath(Constans.SERVER_PATH);
				//客户端加上ZK监听服务器的变化
				CuratorWatcher watcher = new ServerWatcher();
				client.getChildren().usingWatcher(watcher ).forPath(Constans.SERVER_PATH);
				
				for(String path :serverPath){
					String[] str = path.split("#");
					ChannelManager.realServerPath.add(str[0]+"#"+str[1]);
					ChannelFuture channnelFuture = NettyClient.b.connect(str[0], Integer.valueOf(str[1]));
					ChannelManager.addChnannel(channnelFuture);
				}
				if(ChannelManager.realServerPath.size()>0){
					String[] netMessageArray = ChannelManager.realServerPath.toArray()[0].toString().split("#");
					host = netMessageArray[0];
					port = Integer.valueOf(netMessageArray[1]);
				}
			
//			f = b.connect(host, port).sync();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
     * send()
     * -----------------------------------------------
     * 🔹 功能：
     * 向远程服务端发送一个 RPC 请求（ClientRequest），并同步等待响应。
     *
     * 🔹 执行流程：
     * 1️ 从 ChannelManager 获取一个可用的 Netty Channel；
     * 2️ 将 ClientRequest 序列化为 JSON 字符串；
     * 3️ 写入 Channel 并发送到远程 Provider；
     * 4️ 创建一个 ResultFuture 对象等待响应；
     * 5️ 收到响应后由 SimpleClientHandler 唤醒等待线程；
     * 6️ 返回封装好的 Response。
     *
     * @param request 客户端封装的请求对象（包含方法名、参数等）
     * @return 服务端返回的 Response 对象
     */
	
	public static Response send(ClientRequest request){
		f = ChannelManager.get(ChannelManager.position);
		f.channel().writeAndFlush(JSONObject.toJSONString(request)+"\r\n");
//		f.channel().writeAndFlush("\r\n");
		Long timeOut = 60l;
		ResultFuture future = new ResultFuture(request);
//		return future.get(timeOut);
		return future.get(timeOut);

	}
	
}
