package medium;
/**
 * BeanMethod 是一个封装类，用于保存某个 Bean 实例和它要调用的方法对象。
 *
 * 作用：
 * - 在 RPC 框架中，当框架扫描到带有自定义注解的类和方法时，
 *   会将 Bean 实例和 Method 封装到 BeanMethod 中，存入一个 Map。
 * - 当有请求到达时，可以根据请求找到对应的 BeanMethod，
 *   然后通过反射调用其中保存的方法。
 *
 * 字段说明：
 * - bean   : Spring 容器中的具体 Bean 实例（例如 UserServiceImpl 对象）。
 * - method : 该 Bean 上暴露的某个方法（java.lang.reflect.Method）。
 *
 * 使用场景：
 * - RPC 框架的服务端方法映射；
 * - 通过反射执行具体的业务逻辑。
 */


import java.lang.reflect.Method;

public class BeanMethod {
	private Object bean;
	private Method method;
	
	
	public Object getBean() {
		return bean;
	}
	public void setBean(Object bean) {
		this.bean = bean;
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	
	
}
