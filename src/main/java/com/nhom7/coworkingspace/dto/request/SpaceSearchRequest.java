package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.util.ValidSpaceSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidSpaceSearch
@Schema(description = "Query parameters for searching co-working spaces")
public class SpaceSearchRequest {

    @Size(max = 200, message = "{validation.space.name.size}")
    @Schema(description = "Space or venue name", example = "Meeting Room")
    private String name;

    @Size(max = 100, message = "{validation.space.location.size}")
    @Schema(example = "Da Nang")
    private String city;

    @Size(max = 200, message = "{validation.space.location.size}")
    @Schema(example = "Bach Dang")
    private String street;

    @Size(max = 300, message = "{validation.space.location.size}")
    @Schema(example = "Bach Dang 123, Hai Chau")
    private String address;

    /**
     * Space type (e.g. private office, working desk, meeting space)
     */
    @Size(max = 50, message = "{validation.space.type.size}")
    @Schema(example = "meeting space")
    private String type;

    @DecimalMin(value = "0.0", message = "{validation.space.searchPrice.min}")
    @Schema(example = "100000")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", message = "{validation.space.searchPrice.min}")
    @Schema(example = "500000")
    private BigDecimal maxPrice;

    /**
     * Price unit: hour, day, month (or PER_HOUR, PER_DAY, PER_MONTH)
     */
    @Pattern(
            regexp = "(?i)^(HOUR|DAY|MONTH|PER_HOUR|PER_DAY|PER_MONTH)$",
            message = "{validation.space.priceUnit.invalid}"
    )
    @Schema(
            example = "HOUR",
            allowableValues = {"HOUR", "DAY", "MONTH", "PER_HOUR", "PER_DAY", "PER_MONTH"}
    )
    private String priceUnit;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Schema(type = "string", format = "time", example = "08:00:00")
    private LocalTime openTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Schema(type = "string", format = "time", example = "18:00:00")
    private LocalTime closeTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(type = "string", format = "date-time", example = "2026-08-28T08:00:00")
    private LocalDateTime bookingStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(type = "string", format = "date-time", example = "2026-08-28T10:00:00")
    private LocalDateTime bookingEnd;

    @Builder.Default
    @Min(value = 0, message = "{validation.page.min}")
    @Schema(example = "0", defaultValue = "0", minimum = "0")
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = "{validation.size.min}")
    @Max(value = 100, message = "{validation.size.max}")
    @Schema(example = "10", defaultValue = "10", minimum = "1", maximum = "100")
    private int size = 10;

    @Builder.Default
    @Pattern(
            regexp = "^(id|name|price|createdAt|type|priceUnit)$",
            message = "{validation.space.sortBy.invalid}"
    )
    @Schema(
            example = "id",
            defaultValue = "id",
            allowableValues = {"id", "name", "price", "createdAt", "type", "priceUnit"}
    )
    private String sortBy = "id";

    @Builder.Default
    @Pattern(regexp = "(?i)^(ASC|DESC)$", message = "{validation.sortDir.invalid}")
    @Schema(example = "ASC", defaultValue = "ASC", allowableValues = {"ASC", "DESC"})
    private String sortDir = "ASC";
}
