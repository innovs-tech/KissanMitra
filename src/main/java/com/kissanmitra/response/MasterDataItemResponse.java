package com.kissanmitra.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for master data items (DeviceType/Manufacturer).
 *
 * <p>Used in public dropdown APIs and enriched responses.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MasterDataItemResponse {

    /**
     * Code (immutable identifier).
     */
    private String code;

    /**
     * Display name (editable value).
     */
    private String displayName;

    /**
     * Whether active.
     */
    private Boolean active;
}

