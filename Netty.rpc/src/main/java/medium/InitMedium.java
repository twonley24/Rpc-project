package medium;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import annotation.Remote;
import annotation.RemoteInvoke;
import controller.UserController;

/**
 * InitMedium 是一个 BeanPostProcessor，用于在 Spring 容器初始化 Bean 的过程中，
 * 扫描并注册所有带有 @Remote 注解的服务类和方法。
 *
 * 工作机制：
 * 1. Spring 在启动时会依次实例化所有 Bean；
 * 2. 每当一个 Bean 初始化完成后，Spring 会回调 postProcessAfterInitialization 方法，
 *    并将该 Bean 作为参数传入；
 * 3. 如果该 Bean 的类上存在 @Remote 注解，则获取该类中所有方法，
 *    将 (Bean实例 + Method对象) 封装成 BeanMethod；
 * 4. 将 BeanMethod 保存到 Medium.mediamap 中，key 通常是方法名（或接口名+方法名），
 *    供后续 RPC 请求时通过反射调用；
 * 5. 如果 Bean 没有 @Remote 注解，则直接忽略，不做任何处理。
 *
 * 作用：
 * - 自动收集 RPC 服务端可对外暴露的 Bean 和方法；
 * - 为后续 RPC 请求的动态方法调用提供基础映射；
 * - 相当于在 Spring 启动时自动完成“服务注册”。
 */


@Component
public class InitMedium implements BeanPostProcessor{
	//中介者
	@Override
	public Object postProcessAfterInitialization(Object bean, String arg1) throws BeansException {
		if(bean.getClass().isAnnotationPresent(Remote.class)){
			Method[] methods = bean.getClass().getDeclaredMethods();//客户端那里用的是接口，所以getSuperClass
			for(Method m : methods){
//				String key = bean.getClass().getInterfaces()[0].getName()+"."+m.getName();
				String key = m.getName();//修改
				HashMap<String, BeanMethod> map = Medium.mediamap;
				BeanMethod beanMethod = new BeanMethod();
				beanMethod.setBean(bean);
				beanMethod.setMethod(m);
				map.put(key,beanMethod);
				System.out.println(key);
			}
		}
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String arg1) throws BeansException {
		
		
		return bean;
	}

}
