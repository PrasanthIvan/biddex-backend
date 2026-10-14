package dev.bidding.demo.controller;

import dev.bidding.demo.entity.User;
import dev.bidding.demo.entity.Wallet;
import dev.bidding.demo.repository.UserRepository;
import dev.bidding.demo.repository.WalletRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public AuthController(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    // 1. Sign Up & Direct User Creation
    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String username = payload.get("username");
            String phone = payload.get("phone");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required!"));
            }

            String cleanEmail = email.trim().toLowerCase();

            // Check existing email
            if (userRepository.findByEmail(cleanEmail).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already registered! Please Login with OTP."));
            }

            // Create clean username
            String cleanUsername = (username != null && !username.trim().isEmpty())
                    ? username.trim()
                    : cleanEmail.split("@")[0];

            if (userRepository.findByUsername(cleanUsername).isPresent()) {
                cleanUsername = cleanUsername + "_" + (new Random().nextInt(900) + 100);
            }

            User user = new User();
            user.setEmail(cleanEmail);
            user.setUsername(cleanUsername);
            user.setPhone(phone != null ? phone.trim() : "");
            user.setPassword("DEFAULT_OTP_PASS");
            user.setPasswordHash("DEFAULT_OTP_HASH");

            // 6-digit OTP
            String otp = String.format("%06d", new Random().nextInt(999999));
            user.setOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(15));

            User savedUser = userRepository.save(user);

            // Create Wallet with initial ₹50,000 demo balance
            try {
                Wallet wallet = walletRepository.findByUserId(savedUser.getId()).orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(savedUser);
                    newWallet.setBalance(BigDecimal.valueOf(50000));
                    newWallet.setLockedBalance(BigDecimal.ZERO);
                    return newWallet;
                });
                walletRepository.save(wallet);
            } catch (Exception walletEx) {
                System.out.println("Wallet auto-init notice: " + walletEx.getMessage());
            }

            System.out.println("==================================================");
            System.out.println(">>> [SUCCESS] NEW USER CREATED: " + savedUser.getUsername() + " (ID: " + savedUser.getId() + ")");
            System.out.println(">>> [OTP CODE]: " + otp);
            System.out.println("==================================================");

            return ResponseEntity.ok(Map.of(
                    "message", "User account created! OTP sent.",
                    "email", cleanEmail,
                    "devOtp", otp
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Signup failed: " + e.getMessage()));
        }
    }

    // 2. Send OTP for Login
    @PostMapping(value = "/send-otp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendLoginOtp(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required!"));
            }

            String cleanEmail = email.trim().toLowerCase();
            User user = userRepository.findByEmail(cleanEmail).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found! Please Sign Up first."));
            }

            String otp = String.format("%06d", new Random().nextInt(999999));
            user.setOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            System.out.println("==================================================");
            System.out.println(">>> [LOGIN OTP FOR " + cleanEmail + "]: " + otp);
            System.out.println("==================================================");

            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent.",
                    "email", cleanEmail,
                    "devOtp", otp
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "OTP error: " + e.getMessage()));
        }
    }

    // 3. Verify OTP & Finalize Session
    @PostMapping(value = "/verify-otp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String enteredOtp = payload.get("otp");

            if (email == null || enteredOtp == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP required."));
            }

            String cleanEmail = email.trim().toLowerCase();
            User user = userRepository.findByEmail(cleanEmail).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found!"));
            }

            if (user.getOtp() == null || !user.getOtp().trim().equals(enteredOtp.trim())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP!"));
            }

            if (user.getOtpExpiry() != null && LocalDateTime.now().isAfter(user.getOtpExpiry())) {
                return ResponseEntity.badRequest().body(Map.of("message", "OTP has expired."));
            }

            // Clear OTP
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Verification failed: " + e.getMessage()));
        }
    }
}