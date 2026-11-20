package cn.dianping.platform.coupon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "coupon_campaign")
public class CouponCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "total_stock", nullable = false)
    private int totalStock;

    @Column(name = "available_stock", nullable = false)
    private int availableStock;

    @Column(name = "price_minor", nullable = false)
    private long priceMinor;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CouponCampaignStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CouponCampaign() {
    }

    public CouponCampaign(Long merchantId, String name, String description, int totalStock,
                          long priceMinor, Instant startsAt, Instant endsAt) {
        this.merchantId = merchantId;
        this.name = name;
        this.description = description;
        this.totalStock = totalStock;
        this.availableStock = totalStock;
        this.priceMinor = priceMinor;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = CouponCampaignStatus.DRAFT;
    }

    public void activate() {
        if (status != CouponCampaignStatus.DRAFT && status != CouponCampaignStatus.OFFLINE) {
            throw new IllegalStateException("Only draft or offline campaigns can be activated");
        }
        status = CouponCampaignStatus.ACTIVE;
    }

    public void offline() {
        if (status != CouponCampaignStatus.ACTIVE) {
            throw new IllegalStateException("Only active campaigns can be taken offline");
        }
        status = CouponCampaignStatus.OFFLINE;
    }

    public void reserveOne() {
        if (availableStock <= 0) {
            throw new IllegalStateException("Campaign stock is exhausted");
        }
        availableStock--;
    }

    public void releaseOne() {
        if (availableStock < totalStock) {
            availableStock++;
        }
    }

    public Long getId() { return id; }
    public Long getMerchantId() { return merchantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTotalStock() { return totalStock; }
    public int getAvailableStock() { return availableStock; }
    public long getPriceMinor() { return priceMinor; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public CouponCampaignStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
