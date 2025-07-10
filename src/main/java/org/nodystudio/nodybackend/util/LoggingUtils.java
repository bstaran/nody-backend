package org.nodystudio.nodybackend.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * 로깅 시 개인 식별 가능 정보(PII) 보호를 위한 유틸리티 클래스
 * 
 * 이 클래스는 사용자 ID, 이메일, 닉네임 등의 민감한 정보를
 * 안전하게 로그에 기록할 수 있도록 마스킹 및 해시 기능을 제공합니다.
 */
public final class LoggingUtils {

  private LoggingUtils() {
    // 유틸리티 클래스 인스턴스화 방지
  }

  /**
   * 사용자 ID를 안전하게 로그에 기록하기 위해 마스킹 처리합니다.
   * 
   * @param userId 사용자 ID
   * @return 마스킹된 사용자 ID (예: "user_****1234")
   */
  public static String maskUserId(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      return "[EMPTY_USER_ID]";
    }
    
    String trimmed = userId.trim();
    if (trimmed.length() <= 4) {
      return "user_****";
    }
    
    String prefix = "user_";
    String suffix = trimmed.substring(trimmed.length() - 4);
    return prefix + "****" + suffix;
  }

  /**
   * 사용자 ID (Long)를 안전하게 로그에 기록하기 위해 마스킹 처리합니다.
   * 
   * @param userId 사용자 ID (Long)
   * @return 마스킹된 사용자 ID
   */
  public static String maskUserId(Long userId) {
    if (userId == null) {
      return "[NULL_USER_ID]";
    }
    return maskUserId(userId.toString());
  }

  /**
   * 이메일 주소를 마스킹 처리합니다.
   * 
   * @param email 이메일 주소
   * @return 마스킹된 이메일 (예: "u***@e***.com")
   */
  public static String maskEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      return "[EMPTY_EMAIL]";
    }
    
    String trimmed = email.trim();
    int atIndex = trimmed.indexOf('@');
    
    if (atIndex <= 0 || atIndex >= trimmed.length() - 1) {
      return "[INVALID_EMAIL]";
    }
    
    String localPart = trimmed.substring(0, atIndex);
    String domainPart = trimmed.substring(atIndex + 1);
    
    String maskedLocal = maskString(localPart, 1, 1);
    String maskedDomain = maskDomain(domainPart);
    
    return maskedLocal + "@" + maskedDomain;
  }

  /**
   * 닉네임을 마스킹 처리합니다.
   * 
   * @param nickname 닉네임
   * @return 마스킹된 닉네임 (예: "홍*동")
   */
  public static String maskNickname(String nickname) {
    if (nickname == null || nickname.trim().isEmpty()) {
      return "[EMPTY_NICKNAME]";
    }
    
    String trimmed = nickname.trim();
    return maskString(trimmed, 1, 1);
  }

  /**
   * 외부 서비스 식별자 (Provider ID, Social ID 등)를 마스킹 처리합니다.
   * 
   * @param identifier 외부 식별자
   * @return 마스킹된 식별자 (예: "123****890")
   */
  public static String maskIdentifier(String identifier) {
    if (identifier == null || identifier.trim().isEmpty()) {
      return "[EMPTY_IDENTIFIER]";
    }
    
    String trimmed = identifier.trim();
    return maskString(trimmed, 3, 3);
  }

  /**
   * 문자열을 해시값으로 변환합니다. (개발/디버그 환경에서만 사용)
   * 
   * @param input 원본 문자열
   * @return SHA-256 해시값의 첫 8자리
   */
  public static String toSafeHash(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "[EMPTY_INPUT]";
    }
    
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < Math.min(4, hash.length); i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      
      return "hash_" + hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      return "[HASH_ERROR]";
    }
  }

  /**
   * 문자열의 일부분을 마스킹 처리합니다.
   * 
   * @param input 원본 문자열
   * @param prefixLength 앞에서부터 보여줄 문자 수
   * @param suffixLength 뒤에서부터 보여줄 문자 수
   * @return 마스킹된 문자열
   */
  private static String maskString(String input, int prefixLength, int suffixLength) {
    if (input.length() <= prefixLength + suffixLength) {
      return "*".repeat(input.length());
    }
    
    String prefix = input.substring(0, prefixLength);
    String suffix = input.substring(input.length() - suffixLength);
    int maskLength = input.length() - prefixLength - suffixLength;
    
    return prefix + "*".repeat(maskLength) + suffix;
  }

  /**
   * 도메인 부분을 마스킹 처리합니다.
   * 
   * @param domain 도메인 문자열
   * @return 마스킹된 도메인
   */
  private static String maskDomain(String domain) {
    int dotIndex = domain.lastIndexOf('.');
    
    if (dotIndex <= 0 || dotIndex >= domain.length() - 1) {
      return maskString(domain, 1, 1);
    }
    
    String domainName = domain.substring(0, dotIndex);
    String tld = domain.substring(dotIndex);
    
    String maskedDomainName = maskString(domainName, 1, 1);
    return maskedDomainName + tld;
  }

  /**
   * 운영 환경에서 안전한 사용자 식별을 위한 로그 메시지를 생성합니다.
   * 
   * @param action 수행된 작업
   * @param userId 사용자 ID
   * @return 안전한 로그 메시지
   */
  public static String createSafeUserLog(String action, String userId) {
    return String.format("%s: userId=%s", action, maskUserId(userId));
  }

  /**
   * 운영 환경에서 안전한 사용자 식별을 위한 로그 메시지를 생성합니다.
   * 
   * @param action 수행된 작업
   * @param userId 사용자 ID (Long)
   * @return 안전한 로그 메시지
   */
  public static String createSafeUserLog(String action, Long userId) {
    return String.format("%s: userId=%s", action, maskUserId(userId));
  }
}