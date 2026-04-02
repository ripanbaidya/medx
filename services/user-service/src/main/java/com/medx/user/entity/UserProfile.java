package com.medx.user.entity;

import com.medx.user.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Table
@Entity(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private String userId;

    // 1-1 mapped by user_id
    // @MapsId means UserProfile shares the same primary key as User, so user_id is both PK and FK to User
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    // Cloudinary URL - stored after successful upload, null if patient hasn't uploaded photo yet!
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    // Cloudinary public_id - used for deleting the photo from Cloudinary when patient remove or replace it.
    @Column(name = "profile_photo_public_id")
    private String profilePhotoPublicId;

    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "address_street"))
    @AttributeOverride(name = "city", column = @Column(name = "address_city"))
    @AttributeOverride(name = "state", column = @Column(name = "address_state"))
    @AttributeOverride(name = "country", column = @Column(name = "address_country"))
    @AttributeOverride(name = "pinCode", column = @Column(name = "address_pin_code"))
    private Address address;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

}
