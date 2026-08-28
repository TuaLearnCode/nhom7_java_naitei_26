package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.enums.SpaceStatus;
import com.nhom7.coworkingspace.util.ValidOperatingHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidOperatingHours
@Schema(description = "Request body for updating a space")
public class SpaceUpdateRequest {

    @NotBlank(message = "{validation.space.name.required}")
    @Size(max = 200, message = "{validation.space.name.size}")
    private String name;

    @Size(max = 50, message = "{validation.space.type.size}")
    private String type;

    @NotNull(message = "{validation.space.capacity.required}")
    @Min(value = 1, message = "{validation.space.capacity.min}")
    private Integer capacity;

    private String description;

    @NotNull(message = "{validation.space.price.required}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{validation.space.price.min}")
    private BigDecimal price;

    @NotBlank(message = "{validation.space.priceUnit.required}")
    @Pattern(
            regexp = "(?i)^(HOUR|DAY|MONTH|PER_HOUR|PER_DAY|PER_MONTH)$",
            message = "{validation.space.priceUnit.invalid}"
    )
    @Schema(example = "HOUR", allowableValues = {"HOUR", "DAY", "MONTH", "PER_HOUR", "PER_DAY", "PER_MONTH"})
    private String priceUnit;

    @NotNull(message = "{validation.space.openTime.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Schema(type = "string", format = "time", example = "08:00:00")
    private LocalTime openTime;

    @NotNull(message = "{validation.space.closeTime.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Schema(type = "string", format = "time", example = "18:00:00")
    private LocalTime closeTime;

    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private SpaceStatus status;
}
