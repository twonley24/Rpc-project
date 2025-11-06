package bean;

import java.net.InetAddress;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import constants.Constans;
import factory.ZooKeeperFactory;
import handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * NettyInitial
 * -----------------------------------------------
 * 🔹 作用：
 * 这是 RPC 服务端（Provider）端的 **Netty 启动器**，
 * 负责在 Spring 容器启动完成后自动启动 Netty 服务，
 * 建立与客户端的通信通道，并将当前服务节点注册到 Zookeeper。
 *
 * 🔹 核心职责：
 * 1️ 启动 Netty Server，监听指定端口（默认 8080）；
 * 2️ 初始化通信管道 Pipeline（解码、编码、业务处理）；
 * 3️ 向 Zookeeper 注册当前服务节点（用于服务发现）；
 * 4️ 在 Spring 启动完成后自动执行（通过 ApplicationListener 机制）；
 * 5️ 在异常情况下优雅关闭 Netty 线程组。
 *
 * -----------------------------------------------
 * ⚙️ 执行流程：
 * Spring Boot 启动 →
 * ApplicationContext 刷新完成 →
 * onApplicationEvent(ContextRefreshedEvent) 被触发 →
 * 调用 start() 启动 Netty →
 * 注册服务到 Zookeeper →
 * 等待客户端连接。
 *
 * -----------------------------------------------
 * 🔧 技术实现：
 * - 使用 Netty 的 NIO 事件模型（Boss/Worker 线程组）
 * - 使用 ChannelInitializer 配置管道（Decoder → Handler → Encoder）
 * - 使用 CuratorFramework 与 Zookeeper 交互
 * - 使用临时顺序节点 (EPHEMERAL_SEQUENTIAL) 注册服务地址
 * - 与 ServerHandler 协同处理具体业务逻辑
 *
 * -----------------------------------------------
 * @author Taoge
 */

@Component
public class NettyInitial implements ApplicationListener<ContextRefreshedEvent> {
	
	
	/**
     * 启动 Netty Server：
     * - 初始化 Boss/Worker 线程组；
     * - 设置 TCP 参数；
     * - 配置通道处理器；
     * - 绑定端口并监听；
     * - 注册当前服务节点到 Zookeeper。
     */
	public void start() {		
		NioEventLoopGroup boss = new NioEventLoopGroup();
		NioEventLoopGroup work = new NioEventLoopGroup();
			
		try {//启动辅助
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(boss, work)
				   .option(ChannelOption.SO_BACKLOG, 128)//设置TCP队列大小:包含已连接+未连接
				   .option(ChannelOption.SO_KEEPALIVE, false)//不使用默认的心跳机制
				   .channel(NioServerSocketChannel.class)
				   .childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						// 设置\r\n为分隔符
						ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
						ch.pipeline().addLast(new StringDecoder());//字符串解码器
//						ch.pipeline().addLast(new IdleStateHandler(20, 15, 10, TimeUnit.SECONDS));
						ch.pipeline().addLast(new ServerHandler());//业务逻辑处理处
						ch.pipeline().addLast(new StringEncoder());//字符串编码器
					}
				   });
	
			int port = 8080;
			ChannelFuture f = serverBootstrap.bind(8080).sync();
		
			
			// 注册到zk
			InetAddress address = InetAddress.getLocalHost();
			CuratorFramework client = ZooKeeperFactory.getClient();
			if(client != null){
				System.out.println(client);
				client.create().creatingParentsIfNeeded()
				.withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(Constans.SERVER_PATH+"/"+address.getHostAddress()+"#"+port+"#");
				System.out.println("成功");

			}
		
			f.channel().closeFuture().sync();
		
			System.out.println("Closed");
		} catch (Exception e) {
			e.printStackTrace();
			boss.shutdownGracefully();
			work.shutdownGracefully();
		}
	
	}

	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0) {
		this.start();		
	}
	
	
	
}
