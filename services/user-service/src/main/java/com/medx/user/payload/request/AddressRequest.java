package com.medx.user.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(
    name = "AddressRequest",
    description = "Request payload for address"
)
@Builder
public record AddressRequest(

    String street,
    String city,
    String state,
    String country,
    String pinCode

) {
}
