package com.kissanmitra.service.impl;

import com.kissanmitra.entity.VleProfile;
import com.kissanmitra.repository.VleProfileRepository;
import com.kissanmitra.service.VleProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for VLE profile management.
 *
 * <p>Business Context:
 * - VLE profiles are created by Admin
 * - Linked to User entities
 * - Used for lease management and farmer order routing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VleProfileServiceImpl implements VleProfileService {

    private final VleProfileRepository vleProfileRepository;

    /**
     * Creates a new VLE profile.
     *
     * @param vleProfile VLE profile to create
     * @return created VLE profile
     */
    @Override
    public VleProfile createVleProfile(final VleProfile vleProfile) {
        // BUSINESS DECISION: Validate userId uniqueness
        if (vleProfileRepository.findByUserId(vleProfile.getUserId()).isPresent()) {
            throw new RuntimeException("VLE profile already exists for this user");
        }

        final VleProfile saved = vleProfileRepository.save(vleProfile);
        log.info("Created VLE profile: {} for user: {}", saved.getId(), saved.getUserId());
        return saved;
    }

    /**
     * Gets VLE profile by ID.
     *
     * @param id VLE profile ID
     * @return VLE profile
     */
    @Override
    public VleProfile getVleProfileById(final String id) {
        return vleProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VLE profile not found"));
    }

    /**
     * Gets VLE profile by user ID.
     *
     * @param userId user ID
     * @return VLE profile, or null if not found
     */
    @Override
    public VleProfile getVleProfileByUserId(final String userId) {
        return vleProfileRepository.findByUserId(userId)
                .orElse(null);
    }

    /**
     * Gets all VLE profiles.
     *
     * @return list of VLE profiles
     */
    @Override
    public List<VleProfile> getAllVleProfiles() {
        return vleProfileRepository.findAll();
    }
}

