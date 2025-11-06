package Yin.rpc.cousumer.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.ChannelFuture;

/**
 * ChannelManager
 * -----------------------------------------------
 * 🔹 作用：
 * 这是客户端（Consumer）侧的 Netty 连接管理器，
 * 负责统一管理与所有服务端（Provider）建立的 Channel 连接，
 * 并提供 负载均衡（默认采用轮询）机制选择可用通道。
 *
 * 🔹 核心职责：
 * 1️ 维护当前所有已连接的 Channel 列表；
 * 2️ 提供添加、删除、清空连接的方法；
 * 3️ 在发送请求时，通过 get() 方法采用轮询机制选择目标 Channel；
 * 4️ 当服务上下线（Zookeeper Watcher 通知）时动态更新连接池；
 * 5️ 实现线程安全，支持高并发环境访问。
 *
 * -----------------------------------------------
 * ⚙️ 使用场景：
 * - 被 NettyClient 调用，用于获取可用通道：
 *   {@code ChannelFuture f = ChannelManager.get(ChannelManager.position);}
 * - 当 ServerWatcher 监听到 Provider 下线时，会调用 removeChannel() 移除通道；
 * - 当新节点上线时，会调用 addChannel() 动态添加连接。
 *
 * -----------------------------------------------
 * 🔧 技术细节：
 * - 使用 CopyOnWriteArrayList 保证并发读写安全；
 * - 使用 AtomicInteger 实现无锁轮询；
 * - 可以扩展为其他负载均衡策略（如随机、权重、最少连接）。
 *
 * @author Taoge
 */
public class ChannelManager {

    /** 存放当前所有可用的 Netty Channel 连接（线程安全） */
    public static CopyOnWriteArrayList<ChannelFuture> channelFutures = new CopyOnWriteArrayList<>();

    /** 存放 Zookeeper 注册中心中真实的服务器节点信息（格式：ip#port） */
    public static CopyOnWriteArrayList<String> realServerPath = new CopyOnWriteArrayList<>();

    /** 当前轮询计数器，用于选择下一个 Channel */
    public static AtomicInteger position = new AtomicInteger(0);

    /**
     * 从连接池中移除一个失效的 Channel。
     * 通常在服务端宕机或 Zookeeper 通知下线时调用。
     *
     * @param channel 需要移除的连接
     */
    public static void removeChnannel(ChannelFuture channel) {
        channelFutures.remove(channel);
    }

    /**
     * 向连接池中添加一个新的 Channel。
     * 通常在新的 Provider 节点注册或重新连接时调用。
     *
     * @param channel 新的连接
     */
    public static void addChnannel(ChannelFuture channel) {
        channelFutures.add(channel);
    }

    /**
     * 清空所有 Channel。
     * 一般在系统关闭或重新初始化时调用。
     */
    public static void clearChnannel() {
        channelFutures.clear();
    }

    /**
     * 获取一个可用的 Channel。
     * 默认采用「轮询算法」在连接列表中循环选择。
     *
     * @param i 当前轮询计数器
     * @return 一个可用的 ChannelFuture，用于发送请求
     */
    public static ChannelFuture get(AtomicInteger i) {

        // 当前可用连接数
        int size = channelFutures.size();

        // 如果已经轮询到末尾，从头开始
        ChannelFuture channelFuture;
        if (i.get() >= size) {
            channelFuture = channelFutures.get(0);
            ChannelManager.position = new AtomicInteger(1); // 重置游标
        } else {
            // 取出当前索引对应的通道并自增计数
            channelFuture = channelFutures.get(i.getAndIncrement());
        }

        return channelFuture;
    }
}

