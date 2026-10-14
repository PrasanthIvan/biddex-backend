package dev.bidding.demo.repository;

import dev.bidding.demo.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingRepository extends JpaRepository<ShippingAddress, Long> {
    Optional<ShippingAddress> findByAuctionId(Long auctionId);
}