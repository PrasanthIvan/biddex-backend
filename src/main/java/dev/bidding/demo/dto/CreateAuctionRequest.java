package dev.bidding.demo.dto;

import java.math.BigDecimal;

public class CreateAuctionRequest {
    private String title;
    private String description;
    private String itemImageUrl;
    private String category;
    private BigDecimal startingPrice;
    private BigDecimal minBidIncrement;
    private Integer durationMinutes;
    private Long sellerId;

    public CreateAuctionRequest() {}

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

    public BigDecimal getMinBidIncrement() { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal minBidIncrement) { this.minBidIncrement = minBidIncrement; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}