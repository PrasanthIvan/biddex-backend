package dev.bidding.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionResponse {
    private Long id;
    private String title;
    private String description;
    private String itemImageUrl;
    private String category;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestBid;
    private BigDecimal minBidIncrement;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long sellerId;
    private String sellerUsername;

    public AuctionResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getItemImageUrl() { return itemImageUrl; }
    public void setItemImageUrl(String itemImageUrl) { this.itemImageUrl = itemImageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public BigDecimal getMinBidIncrement() { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal minBidIncrement) { this.minBidIncrement = minBidIncrement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
}