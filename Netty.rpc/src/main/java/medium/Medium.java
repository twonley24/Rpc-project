package medium;

import java.lang.reflect.Method;
import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;

import model.Response;
import model.ServerRequest;

/**
 * Medium 是 RPC 框架中的“中介者”，负责保存服务端的 BeanMethod 映射，
 * 并在收到客户端请求时，通过反射调用对应的服务方法。
 *
 * 主要职责：
 * 1. 保存服务映射：
 *    - 使用静态 HashMap<String, BeanMethod> mediamap 存储方法映射关系；
 *    - key 通常是方法名（或接口名+方法名），value 是 BeanMethod（封装了 Bean 实例和 Method 对象）；
 *    - 这些映射由 InitMedium 在 Spring 启动时扫描 @Remote 注解自动填充。
 *
 * 2. 处理客户端请求：
 *    - process(ServerRequest request) 方法是真正的远程调用入口；
 *    - 根据 request.getCommand() 从 mediamap 中找到对应的 BeanMethod；
 *    - 使用 FastJSON 将请求参数反序列化为目标方法需要的参数对象；
 *    - 通过反射调用目标方法，并将结果封装为 Response 返回。
 *
 * 3. 单例模式：
 *    - 使用懒汉式单例（newInstance）保证只有一个 Medium 实例；
 *    - 实际上核心数据 mediamap 已经是 static，全局唯一。
 *
 * 使用场景：
 * - Netty 服务端收到客户端请求后，ServerHandler 会调用 Medium.process()；
 * - Medium 根据请求找到目标方法并执行，最终把结果返回给客户端；
 * - 相当于整个 RPC 框架的“调度中心”，真正实现了远程调用。
 */



public class Medium {
	public static final HashMap<String, BeanMethod> mediamap = new HashMap<String,BeanMethod>();
	private static Medium media = null;
	
	
	private Medium(){}
	
	public static Medium newInstance(){
		if(media == null){
			media = new Medium();
		}
		
		return media;
	}
	
	public Response process(ServerRequest request){
		Response result = null;
		try {
			String command = request.getCommand();//command是key
			BeanMethod beanMethod = mediamap.get(command);
			if(beanMethod == null){
				return null;
			}
			
			Object bean = beanMethod.getBean();
			Method method = beanMethod.getMethod();
			Class type = method.getParameterTypes()[0];//先只实现1个参数的方法
			Object content = request.getContent();
			Object args = JSONObject.parseObject(JSONObject.toJSONString(content), type);
			
			result = (Response) method.invoke(bean, args);
			result.setId(request.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
		
	}
}
