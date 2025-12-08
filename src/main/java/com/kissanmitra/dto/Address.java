package com.kissanmitra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {
    private String firstLine;
    private String secondLine;
    private String pinCode;
    private String city;
    private String state;
    private String country;
}
