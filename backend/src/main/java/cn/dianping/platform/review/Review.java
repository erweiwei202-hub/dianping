package cn.dianping.platform.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

@Entity
@DynamicUpdate
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private short rating;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    protected Review() {
    }

    public Review(long merchantId, long userId, short rating, String content) {
        this.merchantId = merchantId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.status = ReviewStatus.PENDING;
        this.likeCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public Long getUserId() {
        return userId;
    }

    public short getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDeleted() {
        return deletedAt != null || status == ReviewStatus.DELETED;
    }

    public void revise(short rating, String content) {
        this.rating = rating;
        this.content = content;
        this.status = ReviewStatus.PENDING;
        this.updatedAt = Instant.now();
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.status = ReviewStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
        this.likeCount = 0;
    }
}
