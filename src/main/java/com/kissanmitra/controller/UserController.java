package com.kissanmitra.controller;

import com.kissanmitra.enums.Response;
import com.kissanmitra.request.UserRequest;
import com.kissanmitra.response.BaseClientResponse;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kissanmitra.util.CommonUtils.generateRequestId;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/update")
    public BaseClientResponse<UserResponse> updateUserInfo(@RequestBody UserRequest userRequest){
        UserResponse userResponse=userService.updateUserInfo(userRequest);
        return Response.UPDATED.buildSuccess(generateRequestId(),userResponse);
    }

    @GetMapping("/get-user/{id}")
    public BaseClientResponse<UserResponse> getUserDetails(@PathVariable String id){
        UserResponse userResponse=userService.getUserByUserId(id);
        return Response.SUCCESS.buildSuccess(generateRequestId(), userResponse);
    }

}
