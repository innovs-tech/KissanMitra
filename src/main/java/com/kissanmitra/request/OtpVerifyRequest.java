package com.kissanmitra.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class OtpVerifyRequest {
    @NotBlank( message = "Phone Number is Required")
    private String phoneNumber;

    @NotBlank( message = "Otp is Required")
    private String otp;
}
