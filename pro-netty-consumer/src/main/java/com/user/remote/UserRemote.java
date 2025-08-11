package com.user.remote;

import java.util.List;

import com.client.param.Response;
import com.user.bean.User;


public interface UserRemote {
	public Response saveUser(User user);
	public Response saveUsers(List<User> users);

}
