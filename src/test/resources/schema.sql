-- H2 테스트 데이터베이스 초기화 스크립트
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    refresh_token VARCHAR(500),
    refresh_token_expiry TIMESTAMP,
    role VARCHAR(20) DEFAULT 'USER' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_provider_social (provider, social_id),
    UNIQUE KEY unique_email (email)
);
