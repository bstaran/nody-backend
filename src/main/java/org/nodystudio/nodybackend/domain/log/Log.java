package org.nodystudio.nodybackend.domain.log;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.user.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "logs", indexes = {
    @Index(name = "idx_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_logs_location", columnList = "latitude, longitude"),
    @Index(name = "idx_logs_is_public", columnList = "is_public"),
    @Index(name = "idx_logs_created_at", columnList = "created_at")
})
public class Log extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "작성자는 필수입니다.")
    private User user;

    @Column(name = "content", columnDefinition = "TEXT")
    @Size(max = 2000, message = "로그 내용은 2000자를 초과할 수 없습니다.")
    private String content;

    @Column(name = "latitude", precision = 10, scale = 7)
    @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다.")
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다.")
    private BigDecimal longitude;

    @Column(name = "address", length = 500)
    private String address;

    @ElementCollection
    @CollectionTable(
        name = "log_media_urls",
        joinColumns = @JoinColumn(name = "log_id")
    )
    @Column(name = "media_url", length = 500)
    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    @Column(name = "is_public", nullable = false)
    @ColumnDefault("true")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "view_count", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Long viewCount = 0L;

    /**
     * 로그 내용을 업데이트합니다.
     */
    public void updateContent(String content) {
        this.content = content;
    }

    /**
     * 위치 정보를 업데이트합니다.
     */
    public void updateLocation(BigDecimal latitude, BigDecimal longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    /**
     * 공개 설정을 업데이트합니다.
     */
    public void updatePublicSetting(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * 미디어 URL 목록을 업데이트합니다.
     */
    public void updateMediaUrls(List<String> mediaUrls) {
        this.mediaUrls.clear();
        if (mediaUrls != null) {
            this.mediaUrls.addAll(mediaUrls);
        }
    }

    /**
     * 조회수를 증가시킵니다.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 현재 사용자가 이 로그의 작성자인지 확인합니다.
     */
    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    /**
     * 현재 사용자가 이 로그를 볼 수 있는지 확인합니다.
     * - 공개 로그는 모든 사용자가 조회 가능
     * - 비공개 로그는 작성자만 조회 가능
     */
    public boolean isViewableBy(User viewer) {
        if (this.isPublic) {
            return true;
        }
        return viewer != null && isOwnedBy(viewer);
    }
}