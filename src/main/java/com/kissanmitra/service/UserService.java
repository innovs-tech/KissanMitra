package com.kissanmitra.service;

import com.kissanmitra.request.UserRequest;
import com.kissanmitra.response.UserResponse;

public interface UserService {

    UserResponse ensureUserExistsOrSaveUser(String phoneNumber);

    UserResponse updateUserInfo(UserRequest userRequest);

    UserResponse getUserByUserId(String id);
}
