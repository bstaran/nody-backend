-- MySQL TestContainers 용 스키마 파일
-- 이 파일은 TestContainers MySQL 컨테이너가 시작될 때 실행됩니다.

-- 사용자 테이블 생성
CREATE TABLE users
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider          VARCHAR(255) NOT NULL,
    social_id         VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    nickname          VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_provider_social_id (provider, social_id),
    UNIQUE KEY unique_email (email)
);

-- 스레드 테이블 생성
CREATE TABLE threads
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 로그 테이블 생성
CREATE TABLE logs
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 좋아요 테이블 생성
CREATE TABLE likes
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id   BIGINT      NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_target (user_id, target_type, target_id)
);

-- 코멘트 테이블 생성
CREATE TABLE comments
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id   BIGINT      NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 로그 미디어 URL 테이블 생성
CREATE TABLE log_media_urls
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_id     BIGINT       NOT NULL,
    media_url  VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (log_id) REFERENCES logs (id) ON DELETE CASCADE
);

-- 코멘트 멘션 테이블 생성
CREATE TABLE comment_mentions
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id        BIGINT    NOT NULL,
    mentioned_user_id BIGINT    NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    FOREIGN KEY (mentioned_user_id) REFERENCES users (id) ON DELETE CASCADE
);
