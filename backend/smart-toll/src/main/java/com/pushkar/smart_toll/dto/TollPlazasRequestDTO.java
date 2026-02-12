package com.pushkar.smart_toll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TollPlazasRequestDTO {
    @NotBlank(message = "Source pincode cannot be empty")
    @Pattern(regexp = "^[0-9]{6}$", message = "Source pincode must be a 6-digit number")
    private String sourcePincode;

    @NotBlank(message = "Destination pincode cannot be empty")
    @Pattern(regexp = "^[0-9]{6}$", message = "Destination pincode must be a 6-digit number")
    private String destinationPincode;
}
