package com.kissanmitra.repository;

import com.kissanmitra.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /**
     * Finds all users with ADMIN or SUPER_ADMIN role.
     *
     * @return list of admin users
     */
    @Query("{ 'roles': { $in: ['ADMIN', 'SUPER_ADMIN'] } }")
    List<User> findAllAdmins();
}
