-- H2 테스트 데이터베이스 초기화 스크립트
CREATE TABLE IF NOT EXISTS users
(
    user_id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    provider
    VARCHAR
(
    50
) NOT NULL,
    social_id VARCHAR
(
    255
) NOT NULL,
    email VARCHAR
(
    255
),
    nickname VARCHAR
(
    50
) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    refresh_token VARCHAR
(
    500
),
    refresh_token_expiry TIMESTAMP,
    role VARCHAR
(
    20
) DEFAULT 'USER' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY unique_provider_social
(
    provider,
    social_id
),
    UNIQUE KEY unique_email
(
    email
)
    );

CREATE TABLE IF NOT EXISTS logs
(
    log_id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL,
    content
    TEXT,
    latitude
    DECIMAL
(
    10,
    8
),
    longitude DECIMAL
(
    11,
    8
),
    address VARCHAR
(
    255
),
    is_public BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY
(
    user_id
) REFERENCES users
(
    user_id
)
    );

CREATE TABLE IF NOT EXISTS threads
(
    thread_id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL,
    log_id
    BIGINT,
    content
    TEXT
    NOT
    NULL,
    is_public
    BOOLEAN
    DEFAULT
    TRUE
    NOT
    NULL,
    view_count
    BIGINT
    DEFAULT
    0
    NOT
    NULL,
    created_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP,
    updated_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP
    ON
    UPDATE
    CURRENT_TIMESTAMP,
    FOREIGN
    KEY
(
    user_id
) REFERENCES users
(
    user_id
),
    FOREIGN KEY
(
    log_id
) REFERENCES logs
(
    log_id
)
    );
