package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.entity.BargainActivity;
import lut.cn.c2cplatform.entity.BargainHelp;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.service.BargainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bargain")
public class BargainController {

    @Autowired
    private BargainService bargainService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 发起砍价
     */
    @PostMapping("/start")
    public ResponseEntity<?> startBargain(@RequestBody Map<String, Long> request, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            Long productId = request.get("productId");
            if (productId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "商品ID不能为空"));
            }

            BargainActivity bargainActivity = bargainService.startBargain(user.getId(), productId);
            return ResponseEntity.ok(bargainActivity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 帮助砍价
     */
    @PostMapping("/help/{bargainId}")
    public ResponseEntity<?> helpBargain(@PathVariable Long bargainId, Authentication authentication) {
        try {
            Long helperId = null;
            String helperName = "游客";

            if (authentication != null) {
                String username = authentication.getName();
                User user = userMapper.selectByUsername(username);
                helperId = user.getId();
                helperName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            }

            BargainHelp bargainHelp = bargainService.helpBargain(bargainId, helperId, helperName);
            BargainActivity updatedActivity = bargainService.getBargainActivity(bargainId);

            Map<String, Object> result = new HashMap<>();
            result.put("help", bargainHelp);
            result.put("activity", updatedActivity);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 获取砍价活动详情
     */
    @GetMapping("/{bargainId}")
    public ResponseEntity<?> getBargainActivity(@PathVariable Long bargainId) {
        try {
            BargainActivity bargainActivity = bargainService.getBargainActivity(bargainId);
            if (bargainActivity == null) {
                return ResponseEntity.notFound().build();
            }

            List<BargainHelp> helpList = bargainService.getBargainHelpList(bargainId);

            Map<String, Object> result = new HashMap<>();
            result.put("activity", bargainActivity);
            result.put("helpList", helpList);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 获取用户的砍价活动列表
     */
    @GetMapping("/my-bargains")
    public ResponseEntity<?> getMyBargains(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            List<BargainActivity> bargainActivities = bargainService.getUserBargainActivities(user.getId());
            return ResponseEntity.ok(bargainActivities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 检查用户是否可以为商品发起砍价
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkCanStart(@PathVariable Long productId, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.ok(Map.of("canStart", true));
            }

            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            BargainActivity existingBargain = bargainService.getBargainActivity(productId);
            boolean canStart = existingBargain == null;

            Map<String, Object> result = new HashMap<>();
            result.put("canStart", canStart);
            if (!canStart) {
                result.put("existingBargain", existingBargain);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 放弃砍价，以当前价格购买
     */
    @PostMapping("/abandon-and-buy/{bargainId}")
    public ResponseEntity<?> abandonAndBuy(@PathVariable Long bargainId, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            // 调用service层方法处理放弃砍价并购买
            Map<String, Object> result = bargainService.abandonAndBuy(bargainId, user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 以砍价成功的价格购买
     */
    @PostMapping("/buy/{bargainId}")
    public ResponseEntity<?> buyAtBargainPrice(@PathVariable Long bargainId, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            // 调用service层方法处理砍价成功后的购买
            Map<String, Object> result = bargainService.buyAtBargainPrice(bargainId, user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

