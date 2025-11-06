package future;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.ClientRequest;
import model.Response;

/**
 * ResultFuture 用于在 RPC 客户端保存一次请求对应的异步响应结果。
 *
 * 工作流程：
 * 1. 客户端发出请求时，会根据 requestId 创建一个 ResultFuture，并放入全局 map 中进行保存。
 * 2. 调用 get() 方法时，当前线程会进入阻塞，直到服务端响应返回或超时。
 * 3. 当服务端响应到达时，会调用静态方法 receive(response)，
 *    根据 responseId 找到对应的 ResultFuture，填充响应数据并唤醒等待的线程。
 * 4. 为了防止请求永久挂起，内部有一个守护线程 ClearFutureThread，
 *    定期扫描 map 中的请求，如果超时未返回，就构造一个超时响应并唤醒调用方。
 *
 * 核心功能：
 * - 通过 ConcurrentHashMap<Long, ResultFuture> 维护 requestId 与 ResultFuture 的映射。
 * - 使用 Lock + Condition 实现线程阻塞与唤醒机制。
 * - 支持超时机制，避免客户端无限等待。
 *
 * 使用场景：
 * - 客户端调用远程方法时，先创建 ResultFuture 占位；
 * - 服务端返回结果后，通过 receive() 唤醒调用方线程；
 * - 客户端通过 get() 获取最终的 Response。
 *
 * 类似于 JDK 中的 Future/CompletableFuture，用于实现异步调用的同步化处理。
 */

public class ResultFuture {
	public final static ConcurrentHashMap<Long,ResultFuture> map = new ConcurrentHashMap<Long,ResultFuture>();
	final Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private Response response;
	private Long timeOut = 2*60*1000l;
	private Long start = System.currentTimeMillis();
	
	
	public ResultFuture(ClientRequest request){
		map.put(request.getId(), this);
	}
	
	public Response get(){
		lock.lock();
		try {
			while(!done()){
				condition.await();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			lock.unlock();
		}
		
		return this.response;
	}
	
	public Response get(Long time){
		lock.lock();
		try {
			while(!done()){
				condition.await(time,TimeUnit.SECONDS);
				if((System.currentTimeMillis()-start)>timeOut){
					System.out.println("Future中的请求超时");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			lock.unlock();
		}
		
		return this.response;
		
	}
	
	public static void receive(Response response){
		if(response != null){
			ResultFuture future = map.get(response.getId());
			if(future != null){
				Lock lock = future.lock;
				lock.lock();
				try {
					future.setResponse(response);
					future.condition.signal();
					map.remove(future);//别忘记remove
				} catch (Exception e) {
					e.printStackTrace();
				}finally {
					lock.unlock();
				}
			}

		}
	} 

	private boolean done() {
		if(this.response != null){
			return true;
		}
		return false;
	}

	public Long getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(Long timeOut) {
		this.timeOut = timeOut;
	}

	public Long getStart() {
		return start;
	}


	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}
	
	//清理线程
	static class ClearFutureThread extends Thread{
		@Override
		public void run() {
			Set<Long> ids = map.keySet();
			for(Long id : ids){
				ResultFuture f = map.get(id);
				if(f==null){
					map.remove(f);
				}else if(f.getTimeOut()<(System.currentTimeMillis()-f.getStart()))
				{//链路超时
					Response res = new Response();
					res.setId(id);
					res.setCode("33333");
					res.setMsg("链路超时");
					receive(res);
				}
			}
		}
	}
	
	static{
		ClearFutureThread clearThread = new ClearFutureThread();
		clearThread.setDaemon(true);
		clearThread.start();
	}
	
}
