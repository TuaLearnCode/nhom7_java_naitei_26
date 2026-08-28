package com.nhom7.coworkingspace.dto.response;

import com.nhom7.coworkingspace.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueHostResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private Boolean isIdentityVerified;
    private Boolean isBusinessVerified;
}
