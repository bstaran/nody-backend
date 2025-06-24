package org.nodystudio.nodybackend.service.auth;

/**
 * Access Token과 Refresh Token 쌍을 담는 데이터 클래스
 */
public record TokenPair(String accessToken, String refreshToken) {
}