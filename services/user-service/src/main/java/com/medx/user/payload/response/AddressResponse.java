package com.medx.user.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(
    name = "AddressResponse",
    description = "Response payload for address"
)
@Builder
public record AddressResponse(

    String street,
    String city,
    String state,
    String country,
    String pinCode
    
) {
}