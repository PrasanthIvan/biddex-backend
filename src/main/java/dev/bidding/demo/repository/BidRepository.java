package dev.bidding.demo.repository;

import dev.bidding.demo.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);
    Optional<Bid> findTopByAuctionIdOrderByBidAmountDesc(Long auctionId);
}