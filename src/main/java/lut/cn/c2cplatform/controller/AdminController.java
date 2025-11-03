package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.dto.ChatMessageDTO;
import lut.cn.c2cplatform.entity.ChatMessage;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.entity.Report;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.service.ChatMessageService;
import lut.cn.c2cplatform.service.ProductService;
import lut.cn.c2cplatform.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        // 返回包含角色信息的用户
        return ResponseEntity.ok(userMapper.selectAllWithRoles());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userMapper.deleteById(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting user: " + e.getMessage());
        }
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<lut.cn.c2cplatform.dto.ReportDTO>> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        List<lut.cn.c2cplatform.dto.ReportDTO> reportDTOs = new java.util.ArrayList<>();

        for (Report report : reports) {
            // 获取商品信息
            Product product = productService.getProductById(report.getProductId());
            String productName = product != null ? product.getName() : "未知商品";

            // 获取举报人信息
            User reporter = userMapper.selectById(report.getReporterId());
            String reporterUsername = reporter != null ? reporter.getUsername() : "未知用户";
            String reporterDisplayName = reporter != null ? reporter.getDisplayName() : reporterUsername;

            lut.cn.c2cplatform.dto.ReportDTO dto = lut.cn.c2cplatform.dto.ReportDTO.builder()
                .id(report.getId())
                .productId(report.getProductId())
                .productName(productName)
                .reporterId(report.getReporterId())
                .reporterUsername(reporterUsername)
                .reporterDisplayName(reporterDisplayName)
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();

            reportDTOs.add(dto);
        }

        return ResponseEntity.ok(reportDTOs);
    }

    @PutMapping("/reports/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateReportStatus(@PathVariable Long id, @RequestBody Report report) {
        try {
            reportService.updateReportStatus(id, report.getStatus());
            return ResponseEntity.ok("Report status updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating report status: " + e.getMessage());
        }
    }

    @PutMapping("/products/{productId}/delist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delistProduct(@PathVariable Long productId, @RequestParam Long reportId) {
        try {
            System.out.println("[DELIST] 开始下架商品，产品ID: " + productId + ", 举报ID: " + reportId);

            Product product = productService.getProductById(productId);
            if (product == null) {
                System.err.println("[DELIST] 商品不存在: " + productId);
                return ResponseEntity.notFound().build();
            }

            // 下架商品
            productService.delistProduct(productId);
            System.out.println("[DELIST] 商品已下架: " + product.getName());

            // 通知商品所有者
            try {
                System.out.println("[DELIST] ========== 开始通知商品所有者 ==========");
                System.out.println("[DELIST] 查找商品所有者，用户ID: " + product.getUserId());
                User owner = userMapper.selectById(product.getUserId());

                if (owner == null) {
                    System.err.println("[DELIST] ❌ 错误：找不到用户ID为 " + product.getUserId() + " 的用户");
                } else if (owner.getUsername() == null || owner.getUsername().isEmpty()) {
                    System.err.println("[DELIST] ❌ 错误：用户存在但用户名为空，用户ID: " + owner.getId());
                } else {
                    System.out.println("[DELIST] ✅ 找到所有者: " + owner.getUsername() + " (ID: " + owner.getId() + ")");
                    String ownerMessage = "您的商品 '" + product.getName() + "' 已被管理员下架。原因：违反平台规定。如有疑问，请联系客服。";

                    // 保存消息到数据库
                    ChatMessage savedOwnerMessage = chatMessageService.saveMessage("系统", owner.getUsername(), ownerMessage, true);
                    System.out.println("[DELIST] ✅ 所有者消息已保存到数据库，消息ID: " + savedOwnerMessage.getId());

                    // 构建WebSocket消息
                    ChatMessageDTO ownerResponseDTO = ChatMessageDTO.builder()
                        .sender("系统")
                        .recipient(owner.getUsername())
                        .content(savedOwnerMessage.getContent())
                        .timestamp(savedOwnerMessage.getTimestamp())
                        .isSystemMessage(true)
                        .build();

                    System.out.println("[DELIST] 📤 尝试通过WebSocket发送给所有者: " + owner.getUsername());
                    System.out.println("[DELIST] 消息内容: " + ownerMessage);

                    try {
                        messagingTemplate.convertAndSendToUser(
                            owner.getUsername(),
                            "/queue/private",
                            ownerResponseDTO
                        );
                        System.out.println("[DELIST] ✅✅ 成功发送WebSocket消息给商品所有者: " + owner.getUsername());
                    } catch (Exception wsError) {
                        System.err.println("[DELIST] ⚠️ WebSocket发送失败（消息已保存到数据库）: " + wsError.getMessage());
                        wsError.printStackTrace();
                    }
                }
                System.out.println("[DELIST] ========== 商品所有者通知结束 ==========");
            } catch (Exception e) {
                System.err.println("[DELIST] ❌ 通知商品所有者过程出现异常: " + e.getMessage());
                e.printStackTrace();
            }

            // 通知举报者
            try {
                System.out.println("[DELIST] ========== 开始通知举报者 ==========");
                System.out.println("[DELIST] 查找举报信息，举报ID: " + reportId);
                Report report = reportService.getReportById(reportId);

                if (report == null) {
                    System.err.println("[DELIST] ❌ 错误：找不到ID为 " + reportId + " 的举报记录");
                } else if (report.getReporterId() == null) {
                    System.err.println("[DELIST] ❌ 错误：举报记录存在但举报者ID为空");
                } else {
                    System.out.println("[DELIST] ✅ 找到举报记录，举报者ID: " + report.getReporterId());
                    User reporter = userMapper.selectById(report.getReporterId());

                    if (reporter == null) {
                        System.err.println("[DELIST] ❌ 错误：找不到用户ID为 " + report.getReporterId() + " 的举报者");
                    } else if (reporter.getUsername() == null || reporter.getUsername().isEmpty()) {
                        System.err.println("[DELIST] ❌ 错误：举报者存在但用户名为空，用户ID: " + reporter.getId());
                    } else {
                        System.out.println("[DELIST] ✅ 找到举报者: " + reporter.getUsername() + " (ID: " + reporter.getId() + ")");
                        String reporterMessage = "您举报的商品 '" + product.getName() + "' 已被管理员审核并下架。感谢您对平台的监督！";

                        // 保存消息到数据库
                        ChatMessage savedReporterMessage = chatMessageService.saveMessage("系统", reporter.getUsername(), reporterMessage, true);
                        System.out.println("[DELIST] ✅ 举报者消息已保存到数据库，消息ID: " + savedReporterMessage.getId());

                        // 构建WebSocket消息
                        ChatMessageDTO reporterResponseDTO = ChatMessageDTO.builder()
                            .sender("系统")
                            .recipient(reporter.getUsername())
                            .content(savedReporterMessage.getContent())
                            .timestamp(savedReporterMessage.getTimestamp())
                            .isSystemMessage(true)
                            .build();

                        System.out.println("[DELIST] 📤 尝试通过WebSocket发送给举报者: " + reporter.getUsername());
                        System.out.println("[DELIST] 消息内容: " + reporterMessage);

                        try {
                            messagingTemplate.convertAndSendToUser(
                                reporter.getUsername(),
                                "/queue/private",
                                reporterResponseDTO
                            );
                            System.out.println("[DELIST] ✅✅ 成功发送WebSocket消息给举报者: " + reporter.getUsername());
                        } catch (Exception wsError) {
                            System.err.println("[DELIST] ⚠️ WebSocket发送失败（消息已保存到数据库）: " + wsError.getMessage());
                            wsError.printStackTrace();
                        }
                    }
                }
                System.out.println("[DELIST] ========== 举报者通知结束 ==========");
            } catch (Exception e) {
                System.err.println("[DELIST] ❌ 通知举报者过程出现异常: " + e.getMessage());
                e.printStackTrace();
            }

            // 更新举报状态为已处理
            try {
                System.out.println("[DELIST] 更新举报状态，举报ID: " + reportId);
                reportService.updateReportStatus(reportId, "APPROVED");
                System.out.println("[DELIST] ✅ 举报状态已更新为APPROVED");
            } catch (Exception e) {
                System.err.println("[DELIST] ⚠️ 更新举报状态失败: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("[DELIST] 下架流程完成");
            return ResponseEntity.ok("Product delisted successfully and notifications sent");
        } catch (Exception e) {
            System.err.println("[DELIST] 下架商品失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error delisting product: " + e.getMessage());
        }
    }
}
