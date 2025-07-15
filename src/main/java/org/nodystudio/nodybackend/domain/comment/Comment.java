package org.nodystudio.nodybackend.domain.comment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_thread_id", columnList = "thread_id"),
    @Index(name = "idx_comments_author_id", columnList = "author_id"),
    @Index(name = "idx_comments_parent_id", columnList = "parent_id"),
    @Index(name = "idx_comments_created_at", columnList = "created_at")
})
@SQLDelete(sql = "UPDATE comments SET deleted_at = NOW() WHERE comment_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Comment extends BaseTimeEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thread_id", nullable = false)
  @NotNull(message = "스레드는 필수입니다.")
  private Thread thread;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_id")
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  @NotNull(message = "작성자는 필수입니다.")
  private User author;

  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  @NotNull(message = "내용은 필수입니다.")
  @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다.")
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Comment parent;

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @Builder.Default
  private List<Comment> children = new ArrayList<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "comment_mentions", joinColumns = @JoinColumn(name = "comment_id"), inverseJoinColumns = @JoinColumn(name = "user_id"), indexes = {
      @Index(name = "idx_comment_mentions_comment_id", columnList = "comment_id"),
      @Index(name = "idx_comment_mentions_user_id", columnList = "user_id")
  })
  @Builder.Default
  private Set<User> mentionedUsers = new HashSet<>();

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /**
   * 댓글 내용을 업데이트합니다.
   */
  public void updateContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new IllegalArgumentException("내용은 공백일 수 없습니다.");
    }
    this.content = content.trim();
  }

  /**
   * 멘션된 사용자를 추가합니다.
   */
  public void addMentionedUser(User user) {
    if (user != null) {
      this.mentionedUsers.add(user);
    }
  }

  /**
   * 멘션된 사용자들을 설정합니다.
   */
  public void setMentionedUsers(Set<User> users) {
    this.mentionedUsers.clear();
    if (users != null) {
      this.mentionedUsers.addAll(users);
    }
  }

  /**
   * 현재 사용자가 이 댓글의 작성자인지 확인합니다.
   */
  public boolean isOwnedBy(User user) {
    if (user == null || this.author == null) {
      return false;
    }
    return this.author.getId().equals(user.getId());
  }

  /**
   * 댓글이 삭제되었는지 확인합니다.
   */
  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  /**
   * 대댓글인지 확인합니다.
   */
  public boolean isReply() {
    return this.parent != null;
  }

  /**
   * 최상위 댓글인지 확인합니다.
   */
  public boolean isRootComment() {
    return this.parent == null;
  }

  /**
   * 연관관계 편의 메서드 - 자식 댓글 추가
   */
  public void addChild(Comment child) {
    this.children.add(child);
    child.parent = this;
  }

  /**
   * 연관관계 편의 메서드 - 자식 댓글 제거
   */
  public void removeChild(Comment child) {
    this.children.remove(child);
    child.parent = null;
  }

  /**
   * 스레드 설정 (연관관계 편의 메서드에서만 사용)
   */
  public void setThread(Thread thread) {
    this.thread = thread;
  }

}