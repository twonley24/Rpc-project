package Yin.rpc.cousumer.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Yin.rpc.cousumer.param.ClientRequest;
import Yin.rpc.cousumer.param.Response;

/**
 * ResultFuture
 * -----------------------------------------------
 * 🔹 作用：
 * 这是 RPC 客户端用于实现 **异步转同步调用机制** 的核心类。
 * 当客户端通过 Netty 发送一个请求后，会立即创建对应的 ResultFuture 对象，
 * 并将其放入全局静态 Map（以 requestId 为键）。  
 * 当服务端返回响应时，由客户端的 Handler 调用 receive() 方法，
 * 找到对应的 Future，设置响应结果并唤醒等待线程。
 *
 * 🔹 核心机制：
 * - 客户端发送请求 → 创建 ResultFuture → 阻塞等待结果；
 * - 服务端返回响应 → receive() 唤醒对应 Future；
 * - 超时未返回 → ClearFutureThread 定期清理并返回超时结果。
 *
 * -----------------------------------------------
 * ⚙️ 执行流程：
 * InvokeProxy → NettyClient.send(request)
 *             → new ResultFuture(request)
 *             → future.get(timeout) 阻塞等待
 *             → SimpleClientHandler.receive(response) 唤醒线程
 *             → future 返回 Response
 *
 * -----------------------------------------------
 * 🔧 技术点：
 * - 使用 ReentrantLock + Condition 控制线程阻塞与唤醒；
 * - 使用 ConcurrentHashMap 存储全局 requestId 与 Future 的映射；
 * - 使用守护线程 ClearFutureThread 定期清理超时请求；
 * - 支持设置超时时间，防止请求永久阻塞。
 *
 * -----------------------------------------------
 * @author Taoge
 */

public class ResultFuture {

    /** 存放所有挂起请求的映射表（key = requestId, value = ResultFuture） */
    public final static ConcurrentHashMap<Long, ResultFuture> map = new ConcurrentHashMap<>();

    /** 每个 Future 独立持有的锁对象，用于等待/唤醒 */
    final Lock lock = new ReentrantLock();

    /** 条件变量，用于在结果未返回前阻塞等待 */
    private Condition condition = lock.newCondition();

    /** 服务端返回的响应结果 */
    private Response response;

    /** 请求超时时间（默认 2 分钟） */
    private Long timeOut = 2 * 60 * 1000L;

    /** 请求创建时间，用于超时检测 */
    private Long start = System.currentTimeMillis();

    /**
     * 构造函数：
     * 创建一个新的 Future 对象，并将其注册到全局 Map。
     * @param request 客户端请求对象
     */
    public ResultFuture(ClientRequest request) {
        map.put(request.getId(), this);
    }

    /**
     * 阻塞等待结果返回（无超时版本）。
     * 当前线程会被挂起，直到收到服务端响应。
     * @return 服务端返回的 Response 对象
     */
    public Response get() {
        lock.lock();
        try {
            while (!done()) {
                condition.await(); // 等待唤醒
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            System.out.println(Thread.currentThread().getName() + " get处释放锁！");
        }
        return this.response;
    }

    /**
     * 阻塞等待结果返回（带超时版本）。
     * 若超时仍未收到响应，则返回 null 或超时 Response。
     *
     * @param time 超时时间（毫秒）
     * @return 服务端返回的 Response 或超时 Response
     */
    public Response get(Long time) {
        lock.lock();
        try {
            while (!done()) {
                condition.await(time, TimeUnit.MILLISECONDS);
                if ((System.currentTimeMillis() - start) > time) {
                    // 请求超时，跳出循环
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return this.response;
    }

    /**
     * 服务端响应回调（静态方法）。
     * 当客户端收到服务端响应时调用，用于唤醒等待的线程。
     *
     * @param response 服务端返回的响应对象
     */
    public static void receive(Response response) {
        if (response != null) {
            ResultFuture future = map.get(response.getId());
            if (future != null) {
                Lock lock = future.lock;
                lock.lock();
                try {
                    // 设置响应并唤醒等待线程
                    future.setResponse(response);
                    future.condition.signal();
                    // 从全局 Map 移除已完成的 Future
                    map.remove(future);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 判断是否已完成（收到响应）
     */
    private boolean done() {
        return this.response != null;
    }

    // Getter / Setter
    public Long getTimeOut() { return timeOut; }
    public void setTimeOut(Long timeOut) { this.timeOut = timeOut; }
    public Long getStart() { return start; }
    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }

    /**
     * ClearFutureThread
     * -----------------------------------------------
     * 🔹 作用：
     * 定时清理超时未返回结果的 Future，防止内存泄漏。
     * 若发现请求超时，会自动构造一个超时 Response 并调用 receive() 唤醒等待线程。
     * 
     * 🔹 运行机制：
     * - 在类加载时启动为守护线程；
     * - 周期性扫描 map 中所有未完成的 Future；
     * - 超时则触发 receive()。
     */
    static class ClearFutureThread extends Thread {
        @Override
        public void run() {
            Set<Long> ids = map.keySet();
            for (Long id : ids) {
                ResultFuture f = map.get(id);
                if (f == null) {
                    map.remove(f);
                } else if (f.getTimeOut() < (System.currentTimeMillis() - f.getStart())) {
                    // 超时处理
                    Response res = new Response();
                    res.setId(id);
                    res.setCode("33333");
                    res.setMsg("链路超时");
                    receive(res);
                }
            }
        }
    }

    // 静态代码块：类加载时启动清理守护线程
    static {
        ClearFutureThread clearThread = new ClearFutureThread();
        clearThread.setDaemon(true);
        clearThread.start();
    }
}
