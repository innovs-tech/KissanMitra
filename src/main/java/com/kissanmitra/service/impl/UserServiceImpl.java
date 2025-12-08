package com.kissanmitra.service.impl;

import com.kissanmitra.entity.User;
import com.kissanmitra.enums.UserStatus;
import com.kissanmitra.mapper.UserMapper;
import com.kissanmitra.repository.UserRepository;
import com.kissanmitra.request.UserRequest;
import com.kissanmitra.response.UserResponse;
import com.kissanmitra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse ensureUserExistsOrSaveUser(String phoneNumber){
        User user=userRepository.findByPhoneNumber(phoneNumber);

        if( user == null ){
            user= User.builder()
                    .phoneNumber(phoneNumber)
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            user=userRepository.save(user);
        }
        return userMapper.mapToResponse(user);

    }

    public UserResponse updateUserInfo(UserRequest userRequest) {

        User existingUser = userRepository.findByPhoneNumber(userRequest.getPhoneNumber());

        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

        User updatedUser = existingUser.toBuilder()
                .name(userRequest.getName() != null ? userRequest.getName() : existingUser.getName())
                .address(userRequest.getAddress() != null ? userRequest.getAddress() : existingUser.getAddress())
                .pinCode(userRequest.getPinCode() != null ? userRequest.getPinCode() : existingUser.getPinCode())
                .companyId(userRequest.getCompanyId() != null ? userRequest.getCompanyId() : existingUser.getCompanyId())
                .role(userRequest.getRole() != null ? userRequest.getRole() : existingUser.getRole())
                .updatedAt(Instant.now())
                .build();

        updatedUser=userRepository.save(updatedUser);

        return  userMapper.mapToResponse(updatedUser);
    }


    public UserResponse getUserByUserId(String id){
     User user=userRepository.findById(id).orElseThrow(
             ()-> new RuntimeException("User Not found"));

     return userMapper.mapToResponse(user);
    }
}
