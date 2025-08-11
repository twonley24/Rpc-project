package com.user.remote;

import java.util.List;

import javax.annotation.Resource;

import com.netty.annotation.Remote;
import com.netty.util.Response;
import com.netty.util.ResponseUtil;
import com.user.model.User;
import com.user.service.UserService;

@Remote
public class UserRemoteImpl implements UserRemote{
	@Resource
    private UserService userService;

    public Object saveUser(User user) {
        userService.save(user);
        return ResponseUtil.createSuccessResult(user);
    }
    
    public Object saveUsers(List<User> users) {
    	userService.saveList(users);
    	return ResponseUtil.createSuccessResult(users);
    } 	

}
