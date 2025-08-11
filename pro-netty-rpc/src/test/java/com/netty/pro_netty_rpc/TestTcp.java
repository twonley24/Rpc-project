package com.netty.pro_netty_rpc;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.netty.client.ClientRequest;
import com.netty.client.TcpClient;
import com.netty.util.Response;

public class TestTcp {
    @Test
    public void testGetResponse() {
        ClientRequest request = new ClientRequest();
        request.setContent("测试tcp长连接请求");
        Response resp = TcpClient.send(request);
        System.out.println(resp.getResult());
    }
    
//    @Test
//    public void testSaveUser() {
//        ClientRequest request = new ClientRequest();
//        User u = new User();
//        u.setId(1);
//        u.setName("John doe");
//
//        request.setCommand("com.user.controller.UserController.saveUser");
//        request.setContent(u);
//
//        Response resp = TcpClient.send(request);
//        System.out.println(resp.getResult());
//    }
//    
//    @Test
//    public void testSaveUsers() {
//        ClientRequest request = new ClientRequest();
//        List<User> users = new ArrayList<User>();
//        User u = new User();
//        u.setId(1);
//        u.setName("John doe");
//        users.add(u);
//        request.setCommand("com.user.controller.UserController.saveUsers");
//        request.setContent(users);
//        Response resp = TcpClient.send(request);
//        System.out.println(resp.getResult());
//    }


}
