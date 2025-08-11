package com.pro_basic.service;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSONObject;
import com.client.annotation.RemoteInvoke;
import com.user.model.User;
import com.user.remote.UserRemote;

@Service
public class BasicService {
	@RemoteInvoke
	private UserRemote userRemote;
	
	
    public void testSaveUser() {
		User u = new User();
        u.setId(1);
        u.setName("John doe");
        Object r = userRemote.saveUser(u);
        System.out.println(JSONObject.toJSONString(r)); 
    }
	
}
