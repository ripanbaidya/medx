package com.medx.user.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    private String street;
    private String city;
    private String state;
    private String country;
    private String pinCode;
}
