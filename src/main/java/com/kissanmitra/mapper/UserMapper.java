package com.kissanmitra.mapper;

import com.kissanmitra.entity.User;
import com.kissanmitra.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse mapToResponse(User user);
}
