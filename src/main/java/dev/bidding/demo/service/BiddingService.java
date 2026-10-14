package dev.bidding.demo.service;

import dev.bidding.demo.dto.BidRequest;
import dev.bidding.demo.entity.Auction;
import dev.bidding.demo.entity.Bid;
import dev.bidding.demo.entity.User;
import dev.bidding.demo.entity.Wallet;
import dev.bidding.demo.repository.AuctionRepository;
import dev.bidding.demo.repository.BidRepository;
import dev.bidding.demo.repository.UserRepository;
import dev.bidding.demo.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class BiddingService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public BiddingService(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          UserRepository userRepository,
                          WalletRepository walletRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Bid placeBid(BidRequest bidRequest) {
        Auction auction = auctionRepository.findById(bidRequest.getAuctionId())
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 1. Check if Auction is active
        if (!"ACTIVE".equalsIgnoreCase(auction.getStatus())) {
            throw new RuntimeException("Auction is closed or inactive");
        }

        LocalDateTime now = LocalDateTime.now();
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            auction.setStatus("ENDED");
            auctionRepository.save(auction);
            throw new RuntimeException("Auction has ended");
        }

        User bidder = userRepository.findById(bidRequest.getBidderId())
                .orElseThrow(() -> new RuntimeException("Bidder not found"));

                BigDecimal bidAmount = bidRequest.getAmount() instanceof BigDecimal 
                ? (BigDecimal) bidRequest.getAmount() 
                : new BigDecimal(String.valueOf(bidRequest.getAmount()));

        // 2. Validate Bid Amount vs Current Highest Bid + Min Increment
        BigDecimal currentHighest = auction.getCurrentHighestBid() != null 
                ? auction.getCurrentHighestBid() 
                : auction.getStartingPrice();

        BigDecimal minIncrement = auction.getMinBidIncrement() != null 
                ? auction.getMinBidIncrement() 
                : BigDecimal.valueOf(500);

        BigDecimal minRequired = currentHighest.add(minIncrement);
        if (bidAmount.compareTo(minRequired) < 0) {
            throw new RuntimeException("Bid must be at least ₹" + minRequired);
        }

        // 3. Escrow Wallet Verification & Locking
        Wallet wallet = walletRepository.findByUserId(bidder.getId())
                .orElseThrow(() -> new RuntimeException("Bidder wallet not found"));

        if (wallet.getBalance().compareTo(bidAmount) < 0) {
            throw new RuntimeException("Insufficient wallet balance to place this bid");
        }

        // Lock funds in escrow
        wallet.setBalance(wallet.getBalance().subtract(bidAmount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(bidAmount));
        walletRepository.save(wallet);

        // 4. Anti-Sniping Rule: Extend timer by 30 seconds if bid placed in last 30 seconds
        if (auction.getEndTime() != null) {
            long secondsRemaining = Duration.between(now, auction.getEndTime()).getSeconds();
            if (secondsRemaining <= 30 && secondsRemaining >= 0) {
                auction.setEndTime(auction.getEndTime().plusSeconds(30));
            }
        }

        // 5. Update Auction highest bid
        auction.setCurrentHighestBid(bidAmount);
        auctionRepository.save(auction);

        // 6. Record and Save Bid
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setBidAmount(bidAmount);
        bid.setBidTime(LocalDateTime.now());

        return bidRepository.save(bid);
    }
}