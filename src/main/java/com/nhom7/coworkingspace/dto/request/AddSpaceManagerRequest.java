package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddSpaceManagerRequest {

    @NotNull(message = "{validation.space.manager.id.required}")
    @Positive(message = "{validation.id.positive}")
    @Schema(example = "2", minimum = "1")
    private Long userId;
}
