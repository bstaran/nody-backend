package org.nodystudio.nodybackend.dto.comment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("CommentResponse DTO 테스트")
class CommentResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    /**
     * CommentResponse 테스트 데이터를 생성하는 헬퍼 메서드
     */
    private CommentResponse createCommentResponse(Long id, String content, Long parentId) {
        UserSummaryResponse author = UserSummaryResponse.builder()
            .id(1L)
            .nickname("테스트유저")
            .build();

        return CommentResponse.builder()
            .id(id)
            .content(content)
            .author(author)
            .mentionedUsers(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .parentId(parentId)
            .children(new ArrayList<>())
            .isDeleted(false)
            .build();
    }

    @Nested
    @DisplayName("JSON 직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("단일 댓글 직렬화가 정상적으로 수행된다")
        void singleCommentSerialization_ShouldSucceed() {
            // Given
            CommentResponse comment = createCommentResponse(1L, "테스트 댓글입니다", null);

            // When & Then
            assertDoesNotThrow(() -> {
                String jsonString = objectMapper.writeValueAsString(comment);
                assertThat(jsonString).contains("테스트 댓글입니다");
                assertThat(jsonString).contains("테스트유저");
            });
        }

        @Test
        @DisplayName("계층형 댓글 구조 직렬화가 정상적으로 수행된다")
        void hierarchicalCommentSerialization_ShouldSucceed() {
            // Given
            CommentResponse parentComment = createCommentResponse(1L, "부모 댓글", null);
            CommentResponse childComment1 = createCommentResponse(2L, "자식 댓글 1", 1L);
            CommentResponse childComment2 = createCommentResponse(3L, "자식 댓글 2", 1L);

            // 계층 구조 설정
            parentComment.addChild(childComment1);
            parentComment.addChild(childComment2);

            // When & Then
            assertDoesNotThrow(() -> {
                String jsonString = objectMapper.writeValueAsString(parentComment);
                assertThat(jsonString).contains("부모 댓글");
                assertThat(jsonString).contains("자식 댓글 1");
                assertThat(jsonString).contains("자식 댓글 2");
                assertThat(jsonString).contains("children");
            });
        }

        @Test
        @DisplayName("깊은 계층 구조의 댓글 직렬화가 StackOverflowError 없이 수행된다")
        void deepHierarchyCommentSerialization_ShouldNotCauseStackOverflow() {
            // Given - 5단계 깊이의 댓글 구조 생성
            CommentResponse rootComment = createCommentResponse(1L, "최상위 댓글", null);
            CommentResponse currentComment = rootComment;

            // 5단계 깊이로 댓글 구조 생성
            for (int i = 2; i <= 6; i++) {
                CommentResponse childComment = createCommentResponse((long) i, "댓글 " + i, currentComment.getId());
                currentComment.addChild(childComment);
                currentComment = childComment;
            }

            // When & Then
            assertDoesNotThrow(() -> {
                String jsonString = objectMapper.writeValueAsString(rootComment);
                assertThat(jsonString).contains("최상위 댓글");
                assertThat(jsonString).contains("댓글 2");
                assertThat(jsonString).contains("댓글 6");
            }, "깊은 계층 구조 직렬화에서 StackOverflowError가 발생하지 않아야 합니다");
        }

        @Test
        @DisplayName("대량의 자식 댓글이 있는 경우에도 직렬화가 정상 수행된다")
        void manyChildrenCommentSerialization_ShouldSucceed() {
            // Given
            CommentResponse parentComment = createCommentResponse(1L, "부모 댓글", null);

            // 50개의 자식 댓글 생성
            for (int i = 2; i <= 51; i++) {
                CommentResponse childComment = createCommentResponse((long) i, "자식 댓글 " + i, 1L);
                parentComment.addChild(childComment);
            }

            // When & Then
            assertDoesNotThrow(() -> {
                String jsonString = objectMapper.writeValueAsString(parentComment);
                assertThat(jsonString).contains("부모 댓글");
                assertThat(jsonString).contains("자식 댓글 2");
                assertThat(jsonString).contains("자식 댓글 51");
            }, "대량의 자식 댓글 직렬화가 정상적으로 수행되어야 합니다");
        }
    }

    @Nested
    @DisplayName("JSON 역직렬화 테스트")
    class JsonDeserializationTest {

        @Test
        @DisplayName("단일 댓글 역직렬화가 정상적으로 수행된다")
        void singleCommentDeserialization_ShouldSucceed() throws JsonProcessingException {
            // Given
            String jsonString = """
                {
                    "id": 1,
                    "content": "테스트 댓글입니다",
                    "author": {
                        "id": 1,
                        "nickname": "테스트유저"
                    },
                    "mentionedUsers": [],
                    "createdAt": "2023-01-01T12:00:00",
                    "updatedAt": "2023-01-01T12:00:00",
                    "parentId": null,
                    "children": [],
                    "isDeleted": false
                }
                """;

            // When
            CommentResponse comment = objectMapper.readValue(jsonString, CommentResponse.class);

            // Then
            assertThat(comment.getId()).isEqualTo(1L);
            assertThat(comment.getContent()).isEqualTo("테스트 댓글입니다");
            assertThat(comment.getAuthor().getNickname()).isEqualTo("테스트유저");
            assertThat(comment.getChildren()).isEmpty();
        }

        @Test
        @DisplayName("계층형 댓글 구조 역직렬화가 정상적으로 수행된다")
        void hierarchicalCommentDeserialization_ShouldSucceed() throws JsonProcessingException {
            // Given
            String jsonString = """
                {
                    "id": 1,
                    "content": "부모 댓글",
                    "author": {
                        "id": 1,
                        "nickname": "테스트유저"
                    },
                    "mentionedUsers": [],
                    "createdAt": "2023-01-01T12:00:00",
                    "updatedAt": "2023-01-01T12:00:00",
                    "parentId": null,
                    "children": [
                        {
                            "id": 2,
                            "content": "자식 댓글",
                            "author": {
                                "id": 1,
                                "nickname": "테스트유저"
                            },
                            "mentionedUsers": [],
                            "createdAt": "2023-01-01T12:00:00",
                            "updatedAt": "2023-01-01T12:00:00",
                            "parentId": 1,
                            "children": [],
                            "isDeleted": false
                        }
                    ],
                    "isDeleted": false
                }
                """;

            // When
            CommentResponse comment = objectMapper.readValue(jsonString, CommentResponse.class);

            // Then
            assertThat(comment.getId()).isEqualTo(1L);
            assertThat(comment.getContent()).isEqualTo("부모 댓글");
            assertThat(comment.getChildren()).hasSize(1);

            CommentResponse childComment = comment.getChildren().get(0);
            assertThat(childComment.getId()).isEqualTo(2L);
            assertThat(childComment.getContent()).isEqualTo("자식 댓글");
            assertThat(childComment.getParentId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("@JsonManagedReference 어노테이션 동작 테스트")
    class JsonManagedReferenceTest {

        @Test
        @DisplayName("@JsonManagedReference가 적용된 children 필드가 정상적으로 직렬화된다")
        void jsonManagedReference_ShouldSerializeChildren() throws JsonProcessingException {
            // Given
            CommentResponse parent = createCommentResponse(1L, "부모 댓글", null);
            CommentResponse child = createCommentResponse(2L, "자식 댓글", 1L);
            parent.addChild(child);

            // When
            String json = objectMapper.writeValueAsString(parent);

            // Then
            assertThat(json).contains("children");
            assertThat(json).contains("자식 댓글");

            // 순환 참조 없이 정상적으로 직렬화되었는지 확인
            CommentResponse deserialized = objectMapper.readValue(json, CommentResponse.class);
            assertThat(deserialized.getChildren()).hasSize(1);
            assertThat(deserialized.getChildren().get(0).getContent()).isEqualTo("자식 댓글");
        }
    }
}