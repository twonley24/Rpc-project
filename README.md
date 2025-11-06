# Introduction 

## 基于 Netty、Zookeeper、Spring 的轻量级 RPC 框架

**作者说明：**
 在深入学习 Netty 的过程中，我希望能更好地理解分布式通信的底层原理，因此决定从零实现一个基于 **Netty + Zookeeper + Spring** 的轻量级 RPC 框架。
 本项目在开发过程中让我对网络通信、序列化、线程同步、服务注册发现等核心机制有了更深入的理解。
 如有不足或改进建议，欢迎通过邮箱联系：**ikouq123@163.com**

------

# 🚀 Features

- ⚡ **支持长连接**：基于 Netty NIO 模型实现高性能 TCP 持久连接
- 🔄 **支持异步调用**：基于 `ResultFuture` + `ReentrantLock` + `Condition` 实现异步请求等待与回调
- ❤️ **支持心跳检测**：通过自定义心跳机制与 IdleStateHandler 检测连接状态，防止空闲断连
- 🧠 **支持 JSON 序列化**：替换 JDK 原生序列化，提升性能与可读性
- 🧩 **基于注解的零配置调用**：结合 Spring `BeanPostProcessor` + CGLIB 动态代理实现远程服务注入
- 🗂️ **基于 Zookeeper 的服务注册中心**：实现服务注册、发现与节点变更监听
- 🔌 **支持客户端连接动态管理**：支持断线重连与节点负载均衡
- 📡 **自定义通信协议**：基于 `DelimiterBasedFrameDecoder` + `\r\n` 分隔符解决 TCP 粘包拆包问题
- 🧱 **底层通信基于 Netty 4.x**：事件驱动模型、线程池复用，性能更高、资源占用更低

# Quick Start

### 服务端开发

- **在服务端的Service下添加你自己的Service,并加上@Service注解**

  <pre>
  @Service
  public class TestService {
  	public void test(User user){
  		System.out.println("调用了TestService.test");
  	}
  }
  </pre>

- **生成1个服务接口并生成1个实现该接口的类**

  ###### 接口如下

  <pre>
  public interface TestRemote {
  	public Response testUser(User user);  
  }
  </pre>

  ###### 实现类如下，为你的实现类添加@Remote注解，该类是你真正调用服务的地方，你可以生成自己想返回给客户端的任何形式的Response

  <pre> 
  @Remote
  public class TestRemoteImpl implements TestRemote{
  	@Resource
  	private TestService service;
  	public Response testUser(User user){
  		service.test(user);
  		Response response = ResponseUtil.createSuccessResponse(user);
  		return response;
  	}
  }	
  </pre>


### 客户端开发

- **在客户端生成一个接口，该接口为你要调用的接口**

  <pre>
  public interface TestRemote {
  	public Response testUser(User user);
  }
  </pre>

### 使用

- **在你要调用的地方生成接口形式的属性，为该属性添加@RemoteInvoke注解**

  <pre>
  @RunWith(SpringJUnit4ClassRunner.class)
  @ContextConfiguration(classes=RemoteInvokeTest.class)
  @ComponentScan("\\")
  public class RemoteInvokeTest {
  	@RemoteInvoke
  	public static TestRemote userremote;
  	public static User user;
  	@Test
  	public void testSaveUser(){
  		User user = new User();
  		user.setId(1000);
  		user.setName("张三");
  		userremote.testUser(user);
  	}
  }	
  </pre>

### 结果

- **一万次调用结果**
  <img width="2554" height="1367" alt="image" src="https://github.com/user-attachments/assets/ea12be3a-f0a6-4104-9492-2284256241eb" />

- **十万次调用结果**
<img width="2534" height="1407" alt="image" src="https://github.com/user-attachments/assets/c7bba879-99d4-4d04-850b-68a216312a89" />



# Overview

<img width="1960" height="1084" alt="image" src="https://github.com/user-attachments/assets/4768dfb7-ba8c-4c9d-a66c-71fd2b204291" />

