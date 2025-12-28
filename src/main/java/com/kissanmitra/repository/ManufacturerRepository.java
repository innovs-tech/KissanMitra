package com.kissanmitra.repository;

import com.kissanmitra.entity.Manufacturer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Manufacturer entity.
 */
@Repository
public interface ManufacturerRepository extends MongoRepository<Manufacturer, String> {

    /**
     * Finds a manufacturer by name.
     *
     * @param name manufacturer name
     * @return Optional Manufacturer
     */
    Optional<Manufacturer> findByName(String name);
}

