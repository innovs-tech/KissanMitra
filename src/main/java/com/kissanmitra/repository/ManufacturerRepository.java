package com.kissanmitra.repository;

import com.kissanmitra.entity.Manufacturer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Manufacturer entity.
 */
@Repository
public interface ManufacturerRepository extends MongoRepository<Manufacturer, String> {

    /**
     * Finds a manufacturer by code.
     *
     * @param code manufacturer code
     * @return Optional Manufacturer
     */
    Optional<Manufacturer> findByCode(String code);

    /**
     * Finds manufacturers by code list (batch fetch).
     *
     * @param codes list of manufacturer codes
     * @return list of manufacturers
     */
    List<Manufacturer> findByCodeIn(List<String> codes);

    /**
     * Finds active manufacturers.
     *
     * @param active active status
     * @return list of active manufacturers
     */
    List<Manufacturer> findByActive(Boolean active);

    /**
     * Finds a manufacturer by name (legacy support).
     *
     * @param name manufacturer name
     * @return Optional Manufacturer
     */
    Optional<Manufacturer> findByName(String name);
}

