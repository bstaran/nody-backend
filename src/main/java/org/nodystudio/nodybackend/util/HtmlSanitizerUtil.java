package org.nodystudio.nodybackend.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * HTML Sanitizer 유틸리티 클래스
 *
 * <p>
 * OWASP Java HTML Sanitizer를 사용하여 사용자 입력에서 XSS 공격을 방지합니다.
 * 안전한 HTML 태그만 허용하고 악성 스크립트나 위험한 태그를 제거합니다.
 * </p>
 */
public final class HtmlSanitizerUtil {

    /**
     * 기본 텍스트 포맷팅과 안전한 링크를 허용하는 정책
     * - 기본 포맷팅: b, i, em, strong, br
     * - 안전한 링크: a (href 속성만 허용)
     */
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    /**
     * 텍스트만 허용하는 엄격한 정책 (모든 HTML 태그 제거)
     * 빈 정책을 생성하여 모든 HTML 태그를 제거
     */
    private static final PolicyFactory TEXT_ONLY_POLICY = new HtmlPolicyBuilder().toFactory();

    // 유틸리티 클래스이므로 인스턴스 생성 방지
    private HtmlSanitizerUtil() {
        throw new AssertionError("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 기본 정책으로 HTML을 sanitize합니다.
     * 
     * @param input sanitize할 HTML 문자열
     * @return 안전한 HTML 문자열
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return POLICY.sanitize(input);
    }

    /**
     * 엄격한 정책으로 HTML을 sanitize합니다 (텍스트만 허용).
     * 
     * @param input sanitize할 HTML 문자열
     * @return 모든 HTML 태그가 제거된 안전한 텍스트
     */
    public static String sanitizeTextOnly(String input) {
        if (input == null) {
            return null;
        }
        // 모든 HTML 태그를 제거하고 텍스트만 반환
        return TEXT_ONLY_POLICY.sanitize(input);
    }

    /**
     * 댓글 내용에 특화된 sanitization을 수행합니다.
     * 기본 텍스트 포맷팅은 허용하되, 스크립트와 위험한 태그는 제거합니다.
     * 
     * @param content 댓글 내용
     * @return sanitize된 댓글 내용
     */
    public static String sanitizeCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // 기본 포맷팅과 안전한 링크만 허용
        return POLICY.sanitize(content.trim());
    }

    /**
     * HTML 태그가 포함되어 있는지 확인합니다.
     * 
     * @param input 확인할 문자열
     * @return HTML 태그가 포함되어 있으면 true, 그렇지 않으면 false
     */
    public static boolean containsHtml(String input) {
        if (input == null) {
            return false;
        }
        return input.matches(".*<[^>]+>.*");
    }

    /**
     * 입력 문자열과 sanitize된 결과가 다른지 확인합니다.
     * 
     * @param original 원본 문자열
     * @param sanitized sanitize된 문자열
     * @return 내용이 변경되었으면 true, 그렇지 않으면 false
     */
    public static boolean wasModified(String original, String sanitized) {
        if (original == null && sanitized == null) {
            return false;
        }
        if (original == null || sanitized == null) {
            return true;
        }
        return !original.equals(sanitized);
    }
}