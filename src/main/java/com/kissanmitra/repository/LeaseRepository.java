package com.kissanmitra.repository;

import com.kissanmitra.entity.Lease;
import com.kissanmitra.domain.enums.LeaseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Lease entity.
 */
@Repository
public interface LeaseRepository extends MongoRepository<Lease, String> {

    /**
     * Finds leases by VLE ID and status.
     *
     * @param vleId VLE ID
     * @param status lease status
     * @return list of leases
     */
    List<Lease> findByVleIdAndStatus(String vleId, LeaseStatus status);

    /**
     * Finds leases by device ID.
     *
     * @param deviceId device ID
     * @return list of leases
     */
    List<Lease> findByDeviceId(String deviceId);
}

