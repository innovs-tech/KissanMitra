package com.kissanmitra.repository;

import com.kissanmitra.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by phone number.
     *
     * @param phone phone number
     * @return User entity, or null if not found
     */
    User findByPhone(String phone);
}
