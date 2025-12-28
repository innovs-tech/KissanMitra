package com.kissanmitra.service;

import com.kissanmitra.entity.VleProfile;

import java.util.List;

/**
 * Service interface for VLE profile management.
 */
public interface VleProfileService {

    /**
     * Creates a new VLE profile.
     *
     * @param vleProfile VLE profile to create
     * @return created VLE profile
     */
    VleProfile createVleProfile(VleProfile vleProfile);

    /**
     * Gets VLE profile by ID.
     *
     * @param id VLE profile ID
     * @return VLE profile
     */
    VleProfile getVleProfileById(String id);

    /**
     * Gets VLE profile by user ID.
     *
     * @param userId user ID
     * @return VLE profile, or null if not found
     */
    VleProfile getVleProfileByUserId(String userId);

    /**
     * Gets all VLE profiles.
     *
     * @return list of VLE profiles
     */
    List<VleProfile> getAllVleProfiles();
}

