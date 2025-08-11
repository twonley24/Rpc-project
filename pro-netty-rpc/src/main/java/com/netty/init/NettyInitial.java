package com.netty.init;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.netty.constant.Constants;
import com.netty.factory.ZookeeperFactory;
import com.netty.handler.ServerHandler;
import com.netty.handler.SimpleServerHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

@Component

public class NettyInitial implements ApplicationListener<ContextRefreshedEvent> {
	
	public void start() {
		EventLoopGroup parentGroup = new NioEventLoopGroup();
		EventLoopGroup childGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(parentGroup, childGroup);
			bootstrap.option(ChannelOption.SO_BACKLOG, 128)
					 .childOption(ChannelOption.SO_KEEPALIVE, false)
					 .channel(NioServerSocketChannel.class)
					 .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
			             @Override
			             public void initChannel(SocketChannel ch) throws Exception {
			            	 ch.pipeline().addLast(new DelimiterBasedFrameDecoder(65535, Delimiters.lineDelimiter()[0]));
			            	 ch.pipeline().addLast(new StringDecoder());
			            	 ch.pipeline().addLast(new IdleStateHandler(60, 45, 20, TimeUnit.SECONDS));
			                 ch.pipeline().addLast(new ServerHandler());
			                 ch.pipeline().addLast(new StringEncoder());
			             }
			         });
			int weight = 2;
			ChannelFuture f = bootstrap.bind(8080).sync();

			CuratorFramework client = ZookeeperFactory.create();
			InetAddress netAddress = InetAddress.getLocalHost();

			client.create()
			      .withMode(CreateMode.EPHEMERAL)
			      .forPath(Constants.SERVER_PATH + "/" + netAddress.getHostAddress() + "#" + 8080 + "#" + weight + "#");

			
			f.channel().closeFuture().sync();
		} catch (Exception e) { 
			e.printStackTrace();
			parentGroup.shutdownGracefully();
			childGroup.shutdownGracefully();
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.start();
	}

}
