package dev.bidding.demo.scheduler;

import dev.bidding.demo.entity.Auction;
import dev.bidding.demo.entity.Bid;
import dev.bidding.demo.entity.Order;
import dev.bidding.demo.repository.AuctionRepository;
import dev.bidding.demo.repository.BidRepository;
import dev.bidding.demo.repository.OrderRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionScheduler(AuctionRepository auctionRepository,
                            BidRepository bidRepository,
                            OrderRepository orderRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.orderRepository = orderRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Runs every 5 seconds to automatically close expired auctions
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore("ACTIVE", now);

        for (Auction auction : expiredAuctions) {
            auction.setStatus("CLOSED");
            auctionRepository.save(auction);

            Optional<Bid> winningBidOpt = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId());

            if (winningBidOpt.isPresent()) {
                Bid winningBid = winningBidOpt.get();
                Long winnerId = winningBid.getBidder() != null ? winningBid.getBidder().getId() : 1L;
                Long sellerId = auction.getSeller() != null ? auction.getSeller().getId() : 1L;

                // Create Escrow Order
                Order order = orderRepository.findByAuctionId(auction.getId()).orElseGet(() -> {
                    Order newOrder = new Order();
                    newOrder.setAuctionId(auction.getId());
                    newOrder.setBuyerId(winnerId);
                    newOrder.setSellerId(sellerId);
                    newOrder.setAmount(winningBid.getBidAmount());
                    newOrder.setStatus("ESCROW_LOCKED");
                    newOrder.setDeliveryAddress("Pending Winner Address Input");
                    newOrder.setTrackingNumber("BDX-" + System.currentTimeMillis());
                    newOrder.setCarrier("BlueDart Express");
                    return orderRepository.save(newOrder);
                });

                System.out.println(">>> [AUCTION CLOSED] ID: " + auction.getId() + " | WINNER BID: ₹" + winningBid.getBidAmount());

                // Direct entity broadcast - zero type conflict
                messagingTemplate.convertAndSend("/topic/auction/" + auction.getId() + "/closed", order);

            } else {
                System.out.println(">>> [AUCTION CLOSED - NO BIDS] ID: " + auction.getId());
                messagingTemplate.convertAndSend("/topic/auction/" + auction.getId() + "/closed", "UNSOLD");
            }
        }
    }
}