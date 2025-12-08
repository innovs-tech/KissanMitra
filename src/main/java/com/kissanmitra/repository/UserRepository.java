package com.kissanmitra.repository;

import com.kissanmitra.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User,String> {

    User findByPhoneNumber(String phoneNumber);
}
