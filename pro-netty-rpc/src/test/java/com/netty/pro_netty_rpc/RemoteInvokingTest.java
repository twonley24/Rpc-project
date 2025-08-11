package com.netty.pro_netty_rpc;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netty.annotation.RemoteInvoke;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RemoteInvokingTest.class)
@ComponentScan("com")
public class RemoteInvokingTest {
	
//	@RemoteInvoke
//	private UserRemote userRemote;
//	
//	@Test
//    public void testSaveUser() {
//		User u = new User();
//        u.setId(1);
//        u.setName("John doe");
//        userRemote.saveUser(u);
//    }
//    
//    @Test
//    public void testSaveUsers() {
//    	List<User> users = new ArrayList<User>();
//        User u = new User();
//        u.setId(1);
//        u.setName("John doe");
//        users.add(u);
//        userRemote.saveUsers(users);
//    }

}
