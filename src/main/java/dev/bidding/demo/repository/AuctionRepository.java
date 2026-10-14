package dev.bidding.demo.repository;

import dev.bidding.demo.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatus(String status);
    List<Auction> findByStatusAndEndTimeBefore(String status, LocalDateTime now);
}