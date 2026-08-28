package com.nhom7.coworkingspace.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nhom7.coworkingspace.enums.VenueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body to update a venue's moderation status")
public class UpdateVenueStatusRequest {

    @NotNull(message = "Status must not be null")
    @Schema(description = "New venue status", example = "APPROVE", allowableValues = {"PENDING", "APPROVE", "BLOCKED"})
    private VenueStatus status;

    @Size(max = 500, message = "{venue.block.reason.size}")
    @Schema(description = "Required reason when blocking a venue", example = "Venue violates platform policies")
    private String reason;

    @JsonIgnore
    @AssertTrue(message = "{venue.block.reason.required}")
    public boolean isBlockReasonValid() {
        return status != VenueStatus.BLOCKED
                || (reason != null && !reason.isBlank());
    }
}
