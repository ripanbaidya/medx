CREATE TABLE user_profiles
(
    user_id                 VARCHAR(255) NOT NULL,
    first_name              VARCHAR(255) NOT NULL,
    last_name               VARCHAR(255) NOT NULL,
    date_of_birth           date,
    gender                  VARCHAR(255),
    profile_photo_url       VARCHAR(255),
    profile_photo_public_id VARCHAR(255),
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    address_street          VARCHAR(255),
    address_city            VARCHAR(255),
    address_state           VARCHAR(255),
    address_country         VARCHAR(255),
    address_pin_code        VARCHAR(255),
    CONSTRAINT pk_user_profiles PRIMARY KEY (user_id)
);

CREATE TABLE users
(
    id          VARCHAR(255) NOT NULL,
    keycloak_id VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(255),
    status      VARCHAR(255) NOT NULL,
    deleted     BOOLEAN      NOT NULL,
    deleted_at  TIMESTAMP WITHOUT TIME ZONE,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_keycloak UNIQUE (keycloak_id);

CREATE UNIQUE INDEX idx_users_email ON users (email);

CREATE UNIQUE INDEX idx_users_keycloak_id ON users (keycloak_id);

CREATE INDEX idx_users_phone ON users (phone);

ALTER TABLE user_profiles
    ADD CONSTRAINT FK_USER_PROFILES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);