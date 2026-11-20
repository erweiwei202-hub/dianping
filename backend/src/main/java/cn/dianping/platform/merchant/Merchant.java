package cn.dianping.platform.merchant;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@DynamicUpdate
@Table(name = "merchant")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 80)
    private String category;

    @Column(length = 100)
    private String region;

    @Column(length = 255)
    private String address;

    @Column(length = 1000)
    private String description;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Merchant() {
    }

    public Merchant(String name, String category, String region, String address, String description, Long ownerUserId) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.description = description;
        this.ownerUserId = ownerUserId;
        this.status = MerchantStatus.ACTIVE;
        this.averageRating = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getRegion() {
        return region;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String name, String category, String region, String address, String description, MerchantStatus status, Long ownerUserId) {
        this.name = name;
        this.category = category;
        this.region = region;
        this.address = address;
        this.description = description;
        this.status = status;
        this.ownerUserId = ownerUserId;
        this.updatedAt = Instant.now();
    }
}
