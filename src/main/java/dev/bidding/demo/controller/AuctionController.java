package dev.bidding.demo.controller;

import dev.bidding.demo.dto.AuctionResponse;
import dev.bidding.demo.dto.BidRequest;
import dev.bidding.demo.dto.CreateAuctionRequest;
import dev.bidding.demo.entity.*;
import dev.bidding.demo.repository.*;
import dev.bidding.demo.service.BiddingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final BiddingService biddingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ShippingRepository shippingRepository;
    private final OrderRepository orderRepository;

    public AuctionController(AuctionRepository auctionRepository,
                             BidRepository bidRepository,
                             BiddingService biddingService,
                             SimpMessagingTemplate messagingTemplate,
                             WalletRepository walletRepository,
                             UserRepository userRepository,
                             ShippingRepository shippingRepository,
                             OrderRepository orderRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.biddingService = biddingService;
        this.messagingTemplate = messagingTemplate;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.shippingRepository = shippingRepository;
        this.orderRepository = orderRepository;
    }

    private AuctionResponse toDto(Auction a) {
        AuctionResponse dto = new AuctionResponse();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setItemImageUrl(a.getItemImageUrl());
        dto.setCategory(a.getCategory() != null ? a.getCategory() : "Electronics");
        dto.setStartingPrice(a.getStartingPrice());
        dto.setCurrentHighestBid(a.getCurrentHighestBid());
        dto.setMinBidIncrement(a.getMinBidIncrement());
        dto.setStatus(a.getStatus());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        if (a.getSeller() != null) {
            dto.setSellerId(a.getSeller().getId());
            dto.setSellerUsername(a.getSeller().getUsername());
        }
        return dto;
    }

    // 1. Get all auctions
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuctionResponse>> getAllAuctions() {
        List<AuctionResponse> list = auctionRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // 2. Get single auction
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuctionResponse> getAuctionById(@PathVariable Long id) {
        return auctionRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Bid history
    @GetMapping(value = "/{id}/bids", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Bid>> getBidHistory(@PathVariable Long id) {
        return ResponseEntity.ok(bidRepository.findByAuctionIdOrderByBidTimeDesc(id));
    }

    // 4. Place live bid + Broadcast
    @PostMapping(value = "/bid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> placeBid(@RequestBody BidRequest bidRequest) {
        try {
            Bid savedBid = biddingService.placeBid(bidRequest);
            messagingTemplate.convertAndSend("/topic/auction/" + bidRequest.getAuctionId(), savedBid);
            return ResponseEntity.ok(savedBid);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    // 5. Send Real-Time Chat Message
    @PostMapping(value = "/{auctionId}/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendChatMessage(@PathVariable Long auctionId, @RequestBody Map<String, String> payload) {
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId + "/chat", payload);
        return ResponseEntity.ok(payload);
    }

    // 6. Get User Wallet
    @GetMapping(value = "/wallets/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserWallet(@PathVariable Long userId) {
        return walletRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 7. Create Auction
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAuction(@RequestBody CreateAuctionRequest req) {
        try {
            Auction auction = new Auction();
            auction.setTitle(req.getTitle());
            auction.setDescription(req.getDescription());

            String imgUrl = req.getItemImageUrl();
            if (imgUrl == null || imgUrl.isBlank() || imgUrl.length() > 255) {
                imgUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30";
            }
            auction.setItemImageUrl(imgUrl);
            auction.setCategory(req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory() : "Electronics");

            BigDecimal startingPrice = req.getStartingPrice() != null ? req.getStartingPrice() : BigDecimal.valueOf(1000);
            BigDecimal minInc = req.getMinBidIncrement() != null ? req.getMinBidIncrement() : BigDecimal.valueOf(500);

            auction.setStartingPrice(startingPrice);
            auction.setCurrentHighestBid(startingPrice);
            auction.setMinBidIncrement(minInc);

            User seller = userRepository.findById(req.getSellerId() != null ? req.getSellerId() : 1L).orElse(null);
            if (seller == null) {
                seller = new User();
                seller.setId(req.getSellerId() != null ? req.getSellerId() : 1L);
                seller.setUsername("seller_1");
                seller.setEmail("seller1@biddex.com");
                seller.setPassword("password123");
                seller.setPasswordHash("password_hash_123");
                seller = userRepository.save(seller);
            }
            auction.setSeller(seller);

            auction.setStatus("ACTIVE");
            auction.setStartTime(LocalDateTime.now());
            int duration = req.getDurationMinutes() != null && req.getDurationMinutes() > 0 ? req.getDurationMinutes() : 2;
            auction.setEndTime(LocalDateTime.now().plusMinutes(duration));

            Auction saved = auctionRepository.save(auction);
            return ResponseEntity.ok(toDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error creating auction: " + e.getMessage()));
        }
    }

    // 8. Deposit Funds
    @PostMapping(value = "/wallets/deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> depositFunds(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            User user = userRepository.findById(userId).orElseGet(() -> {
                User newUser = new User();
                newUser.setId(userId);
                newUser.setUsername("user_" + userId);
                newUser.setEmail("user" + userId + "@biddex.com");
                newUser.setPassword("password123");
                newUser.setPasswordHash("password_hash_123");
                return userRepository.save(newUser);
            });

            Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> {
                Wallet newWallet = new Wallet();
                newWallet.setUser(user);
                newWallet.setBalance(BigDecimal.ZERO);
                newWallet.setLockedBalance(BigDecimal.ZERO);
                return newWallet;
            });

            wallet.setBalance(wallet.getBalance().add(amount));
            Wallet updated = walletRepository.save(wallet);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Deposit failed: " + e.getMessage()));
        }
    }

    // 9. Claim Shipping & Create Escrow Order
    @PostMapping(value = "/{auctionId}/claim-shipping", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submitShippingAndCreateOrder(
            @PathVariable Long auctionId,
            @RequestBody ShippingAddress address) {
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("Auction not found"));

            address.setAuctionId(auctionId);
            address.setShipmentStatus("ORDER_PLACED");
            address.setCreatedAt(LocalDateTime.now());
            ShippingAddress savedAddress = shippingRepository.save(address);

            Order order = orderRepository.findByAuctionId(auctionId).orElse(new Order());
            order.setAuctionId(auctionId);
            order.setBuyerId(address.getWinnerId());
            order.setSellerId(auction.getSeller() != null ? auction.getSeller().getId() : 1L);
            order.setAmount(auction.getCurrentHighestBid());
            order.setStatus("ESCROW_LOCKED");
            order.setDeliveryAddress(address.getAddressLine() + ", " + address.getCity() + " - " + address.getPincode());
            order.setTrackingNumber("BDX-" + System.currentTimeMillis());
            order.setCarrier("BlueDart Express");
            orderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "address", savedAddress,
                    "order", order,
                    "message", "Order confirmed! Funds held in Escrow."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Order claim failed: " + e.getMessage()));
        }
    }

    // 10. Get Order Status
    @GetMapping(value = "/{auctionId}/order-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getOrderStatus(@PathVariable Long auctionId) {
        return orderRepository.findByAuctionId(auctionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // 11. Complete Delivery & Release Escrow Funds to Seller
    @PostMapping(value = "/{auctionId}/complete-delivery", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeDeliveryAndReleaseEscrow(@PathVariable Long auctionId) {
        try {
            Order order = orderRepository.findByAuctionId(auctionId)
                    .orElseThrow(() -> new RuntimeException("Order not found for auction"));

            if ("COMPLETED".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Order already completed!"));
            }

            Wallet buyerWallet = walletRepository.findByUserId(order.getBuyerId())
                    .orElseThrow(() -> new RuntimeException("Buyer wallet not found"));

            Wallet sellerWallet = walletRepository.findByUserId(order.getSellerId())
                    .orElseGet(() -> {
                        User seller = userRepository.findById(order.getSellerId()).orElseThrow();
                        Wallet w = new Wallet();
                        w.setUser(seller);
                        w.setBalance(BigDecimal.ZERO);
                        w.setLockedBalance(BigDecimal.ZERO);
                        return w;
                    });

            buyerWallet.setLockedBalance(buyerWallet.getLockedBalance().subtract(order.getAmount()));
            sellerWallet.setBalance(sellerWallet.getBalance().add(order.getAmount()));

            walletRepository.save(buyerWallet);
            walletRepository.save(sellerWallet);

            order.setStatus("COMPLETED");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            return ResponseEntity.ok(Map.of("message", "Product delivered! Escrow funds released to seller successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Escrow release failed: " + e.getMessage()));
        }
    }
}