package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.CreditScoreResponse;
import lut.cn.c2cplatform.service.CreditScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-score")
public class CreditScoreController {

    @Autowired
    private CreditScoreService creditScoreService;

    /**
     * 获取用户信用分
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCreditScore(@PathVariable Long userId) {
        try {
            CreditScoreResponse creditScore = creditScoreService.getUserCreditScore(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", creditScore);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 手动更新信用分（一般由系统自动调用，此接口可用于管理员手动触发）
     */
    @PostMapping("/update/{userId}")
    public ResponseEntity<?> updateCreditScore(@PathVariable Long userId) {
        try {
            creditScoreService.updateCreditScore(userId);
            CreditScoreResponse creditScore = creditScoreService.getUserCreditScore(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "信用分更新成功");
            response.put("data", creditScore);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

