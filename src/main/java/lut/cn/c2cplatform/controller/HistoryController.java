package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ViewHistoryDTO;
import lut.cn.c2cplatform.security.UserDetailsImpl;
import lut.cn.c2cplatform.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for tracking user browsing behavior
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    /**
     * Record a product view
     * Called from frontend when user visits a product detail page
     */
    @PostMapping("/view")
    public ResponseEntity<?> recordView(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            Long productId = request.get("productId");

            if (userId == null) {
                // Allow anonymous browsing, just don't track
                return ResponseEntity.ok(Map.of("success", true, "tracked", false));
            }

            if (productId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
            }

            historyService.recordView(userId, productId);

            return ResponseEntity.ok(Map.of("success", true, "tracked", true));
        } catch (Exception e) {
            System.err.println("Failed to record view: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the request, just log
            return ResponseEntity.ok(Map.of("success", true, "tracked", false));
        }
    }

    /**
     * Get user's browsing history
     */
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            List<ViewHistoryDTO> history = historyService.getUserHistory(userId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            System.err.println("Failed to get history: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve history"));
        }
    }

    /**
     * Get user's interest profile
     */
    @GetMapping("/my-interests")
    public ResponseEntity<?> getMyInterests(
            @RequestParam(defaultValue = "10") int topN,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            Map<String, Integer> interests = historyService.getUserInterestProfile(userId, topN);
            return ResponseEntity.ok(interests);
        } catch (Exception e) {
            System.err.println("Failed to get interests: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve interests"));
        }
    }

    /**
     * Extract user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        }

        return null;
    }
}

