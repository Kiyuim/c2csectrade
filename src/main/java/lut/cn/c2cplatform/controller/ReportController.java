package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.entity.Report;
import lut.cn.c2cplatform.security.UserDetailsImpl;
import lut.cn.c2cplatform.service.ProductService;
import lut.cn.c2cplatform.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProductService productService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReport(@RequestBody Report report, Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            if (userId == null) {
                return new ResponseEntity<>("无法获取用户信息", HttpStatus.UNAUTHORIZED);
            }
            report.setReporterId(userId);
            reportService.createReport(report);
            return new ResponseEntity<>("举报成功，管理员将尽快处理", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // 处理业务异常（如重复举报）
            System.err.println("Error creating report: " + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("Error creating report: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("举报失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
            var user = productService.getUserByUsername(username);
            return user != null ? user.getId() : null;
        } else if (principal instanceof String) {
            var user = productService.getUserByUsername((String) principal);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}
