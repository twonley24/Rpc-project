package Yin.rpc.cousumer.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.HashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import Yin.rpc.cousumer.annotation.RemoteInvoke;
import Yin.rpc.cousumer.core.NettyClient;
import Yin.rpc.cousumer.param.ClientRequest;
import Yin.rpc.cousumer.param.Response;

/**
 * InvokeProxy
 * -----------------------------------------------
 * 🔹 作用：
 * 这是客户端（Consumer）端的一个 BeanPostProcessor，用于在 Spring 容器启动时，
 * 自动扫描带有 @RemoteInvoke 注解的字段，并为这些远程接口生成动态代理对象。
 *
 * 🔹 工作原理：
 * 1. Spring 在 Bean 初始化前，会调用 postProcessBeforeInitialization()。
 * 2. 该方法会扫描每个 Bean 的字段；
 *    若字段上有 @RemoteInvoke 注解，说明这是一个远程服务接口。
 * 3. 使用 CGLIB 的 Enhancer 为该接口创建代理对象（动态代理类）。
 * 4. 当开发者调用这个接口的方法时，代理对象会：
 *      - 拦截方法调用（intercept）
 *      - 封装成 ClientRequest 请求对象（包含方法名和参数）
 *      - 通过 NettyClient 发送到远程服务器
 *      - 等待服务器返回 Response 对象并返回结果
 *
 * 🔹 结果：
 * 开发者在代码中看到的是一次普通的接口调用（userRemote.saveUser(user)），
 * 实际上底层通过动态代理+Netty完成了整个RPC通信。
 *
 * -----------------------------------------------
 * ⚙️ 核心流程：
 * @RemoteInvoke → BeanPostProcessor → Enhancer 代理生成
 * → intercept() 拦截调用 → 封装 ClientRequest → NettyClient.send()
 * → 等待 Response → 返回结果
 *
 * @Author：Taoge
 */


@Component
public class InvokeProxy implements BeanPostProcessor {
	public static Enhancer enhancer = new Enhancer();

	public Object postProcessAfterInitialization(Object bean, String arg1) throws BeansException {
		return bean;
	}
	//对属性的所有方法和属性类型放入到HashMap中
	private void putMethodClass(HashMap<Method, Class> methodmap, Field field) {
		Method[] methods = field.getType().getDeclaredMethods();
		for(Method method : methods){
			methodmap.put(method, field.getType());
		}
		
	}

	public Object postProcessBeforeInitialization(Object bean, String arg1) throws BeansException {
		Field[] fields = bean.getClass().getDeclaredFields();
		for(Field field : fields){
			if(field.isAnnotationPresent(RemoteInvoke.class)){
				field.setAccessible(true);
				

				enhancer.setInterfaces(new Class[]{field.getType()});
				enhancer.setCallback(new MethodInterceptor() {
					
					public Object intercept(Object instance, Method method, Object[] args, MethodProxy proxy) throws Throwable {
						ClientRequest clientRequest = new ClientRequest();
						clientRequest.setContent(args[0]);
//						String command= methodmap.get(method).getName()+"."+method.getName();
						String command = method.getName();//修改
//						System.out.println("InvokeProxy中的Command是:"+command);
						clientRequest.setCommand(command);
						
						Response response = NettyClient.send(clientRequest);
						return response;
					}
				});
				try {
					field.set(bean, enhancer.create());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return bean;
	}

}
