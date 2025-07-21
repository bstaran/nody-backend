package org.nodystudio.nodybackend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HtmlSanitizerUtil 테스트")
class HtmlSanitizerUtilTest {

  @Nested
  @DisplayName("sanitizeCommentContent 메서드")
  class SanitizeCommentContentTest {

    @Test
    @DisplayName("일반 텍스트는 그대로 반환한다")
    void sanitizeCommentContent_WithPlainText_ShouldReturnAsIs() {
      // Given
      String plainText = "안녕하세요. 좋은 글이네요!";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(plainText);

      // Then
      assertThat(result).isEqualTo(plainText);
    }

    @Test
    @DisplayName("악성 스크립트 태그를 제거한다")
    void sanitizeCommentContent_WithScriptTag_ShouldRemoveScript() {
      // Given
      String maliciousContent = "안녕하세요 <script>alert('XSS');</script> 좋은 글이네요!";
      String expected = "안녕하세요  좋은 글이네요!";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(maliciousContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("악성 이미지 태그를 제거한다")
    void sanitizeCommentContent_WithMaliciousImgTag_ShouldRemoveImgTag() {
      // Given
      String maliciousContent = "댓글입니다 <img src=x onerror=alert('XSS')> 계속 텍스트";
      String expected = "댓글입니다  계속 텍스트";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(maliciousContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("iframe 태그를 제거한다")
    void sanitizeCommentContent_WithIframeTag_ShouldRemoveIframe() {
      // Given
      String maliciousContent = "내용 <iframe src='javascript:alert(1)'></iframe> 더 내용";
      String expected = "내용  더 내용";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(maliciousContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("onload 이벤트 핸들러를 제거한다")
    void sanitizeCommentContent_WithOnloadEvent_ShouldRemoveEvent() {
      // Given
      String maliciousContent = "텍스트 <body onload=alert('XSS')>내용</body> 끝";
      String expected = "텍스트 내용 끝";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(maliciousContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("기본 포맷팅 태그는 허용한다")
    void sanitizeCommentContent_WithFormattingTags_ShouldKeepFormatting() {
      // Given
      String formattedContent = "이것은 <b>굵은 글씨</b>이고 <i>기울임</i>입니다. <br>줄바꿈도 됩니다.";
      String expected = "이것은 <b>굵은 글씨</b>이고 <i>기울임</i>입니다. <br />줄바꿈도 됩니다.";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(formattedContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("안전한 링크는 허용한다")
    void sanitizeCommentContent_WithSafeLink_ShouldKeepLink() {
      // Given
      String linkContent = "참고 링크: <a href=\"https://example.com\">example.com</a>";
      String expected = "참고 링크: <a href=\"https://example.com\" rel=\"nofollow\">example.com</a>";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(linkContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("javascript: 프로토콜 링크를 제거한다")
    void sanitizeCommentContent_WithJavascriptLink_ShouldRemoveLink() {
      // Given
      String maliciousLink = "클릭: <a href=\"javascript:alert('XSS')\">여기</a>";
      String expected = "클릭: 여기";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(maliciousLink);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("null 입력에 대해 null을 반환한다")
    void sanitizeCommentContent_WithNull_ShouldReturnNull() {
      // Given
      String nullContent = null;

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(nullContent);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 문자열을 그대로 반환한다")
    void sanitizeCommentContent_WithEmptyString_ShouldReturnEmpty() {
      // Given
      String emptyContent = "";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(emptyContent);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 문자열을 그대로 반환한다")
    void sanitizeCommentContent_WithWhitespaceOnly_ShouldReturnWhitespace() {
      // Given
      String whitespaceContent = "   ";

      // When
      String result = HtmlSanitizerUtil.sanitizeCommentContent(whitespaceContent);

      // Then
      assertThat(result).isEqualTo(whitespaceContent);
    }
  }

  @Nested
  @DisplayName("sanitizeTextOnly 메서드")
  class SanitizeTextOnlyTest {

    @Test
    @DisplayName("모든 HTML 태그를 제거한다")
    void sanitizeTextOnly_WithHtmlTags_ShouldRemoveAllTags() {
      // Given
      String htmlContent = "<b>굵은</b> 글씨와 <i>기울임</i> 그리고 <script>alert('XSS')</script>";
      String expected = "굵은 글씨와 기울임 그리고 ";

      // When
      String result = HtmlSanitizerUtil.sanitizeTextOnly(htmlContent);

      // Then
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("null 입력에 대해 null을 반환한다")
    void sanitizeTextOnly_WithNull_ShouldReturnNull() {
      // Given
      String nullContent = null;

      // When
      String result = HtmlSanitizerUtil.sanitizeTextOnly(nullContent);

      // Then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("containsHtml 메서드")
  class ContainsHtmlTest {

    @Test
    @DisplayName("HTML 태그가 포함된 경우 true를 반환한다")
    void containsHtml_WithHtmlTags_ShouldReturnTrue() {
      // Given
      String htmlContent = "텍스트 <b>굵은글씨</b> 더 텍스트";

      // When
      boolean result = HtmlSanitizerUtil.containsHtml(htmlContent);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("일반 텍스트인 경우 false를 반환한다")
    void containsHtml_WithPlainText_ShouldReturnFalse() {
      // Given
      String plainText = "일반 텍스트입니다.";

      // When
      boolean result = HtmlSanitizerUtil.containsHtml(plainText);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null 입력에 대해 false를 반환한다")
    void containsHtml_WithNull_ShouldReturnFalse() {
      // Given
      String nullContent = null;

      // When
      boolean result = HtmlSanitizerUtil.containsHtml(nullContent);

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("wasModified 메서드")
  class WasModifiedTest {

    @Test
    @DisplayName("내용이 변경된 경우 true를 반환한다")
    void wasModified_WhenContentChanged_ShouldReturnTrue() {
      // Given
      String original = "텍스트 <script>alert('XSS')</script>";
      String sanitized = "텍스트 ";

      // When
      boolean result = HtmlSanitizerUtil.wasModified(original, sanitized);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("내용이 동일한 경우 false를 반환한다")
    void wasModified_WhenContentSame_ShouldReturnFalse() {
      // Given
      String original = "일반 텍스트";
      String sanitized = "일반 텍스트";

      // When
      boolean result = HtmlSanitizerUtil.wasModified(original, sanitized);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("둘 다 null인 경우 false를 반환한다")
    void wasModified_WhenBothNull_ShouldReturnFalse() {
      // Given
      String original = null;
      String sanitized = null;

      // When
      boolean result = HtmlSanitizerUtil.wasModified(original, sanitized);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("하나만 null인 경우 true를 반환한다")
    void wasModified_WhenOneNull_ShouldReturnTrue() {
      // Given
      String original = "텍스트";
      String sanitized = null;

      // When
      boolean result = HtmlSanitizerUtil.wasModified(original, sanitized);

      // Then
      assertThat(result).isTrue();
    }
  }
}