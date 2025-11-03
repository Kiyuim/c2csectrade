package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.entity.BargainActivity;
import lut.cn.c2cplatform.entity.BargainHelp;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.mapper.BargainActivityMapper;
import lut.cn.c2cplatform.mapper.BargainHelpMapper;
import lut.cn.c2cplatform.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class BargainService {

    @Autowired
    private BargainActivityMapper bargainActivityMapper;

    @Autowired
    private BargainHelpMapper bargainHelpMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderService orderService;

    private static final Random random = new Random();

    /**
     * 发起砍价活动
     */
    @Transactional
    public BargainActivity startBargain(Long userId, Long productId) {
        // 检查商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 检查用户是否已经为该商品发起过砍价
        BargainActivity existingBargain = bargainActivityMapper.selectActiveByUserAndProduct(userId, productId);
        if (existingBargain != null) {
            throw new RuntimeException("您已经为该商品发起过砍价，不能重复发起");
        }

        // 创建砍价活动
        BargainActivity bargainActivity = new BargainActivity();
        bargainActivity.setUserId(userId);
        bargainActivity.setProductId(productId);
        bargainActivity.setOriginalPrice(product.getPrice());

        // 目标价格为原价的60%-80%（随机）
        BigDecimal targetRatio = BigDecimal.valueOf(0.6 + random.nextDouble() * 0.2);
        BigDecimal targetPrice = product.getPrice().multiply(targetRatio).setScale(2, RoundingMode.HALF_UP);
        bargainActivity.setTargetPrice(targetPrice);

        bargainActivity.setCurrentPrice(product.getPrice());
        bargainActivity.setStatus("ACTIVE");

        // 过期时间为24小时后
        Date expireTime = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        bargainActivity.setExpireTime(expireTime);

        bargainActivity.setCreatedAt(new Date());
        bargainActivity.setUpdatedAt(new Date());

        bargainActivityMapper.insert(bargainActivity);

        return bargainActivityMapper.selectById(bargainActivity.getId());
    }

    /**
     * 帮助砍价
     */
    @Transactional
    public BargainHelp helpBargain(Long bargainId, Long helperId, String helperName) {
        // 检查砍价活动是否存在
        BargainActivity bargainActivity = bargainActivityMapper.selectById(bargainId);
        if (bargainActivity == null) {
            throw new RuntimeException("砍价活动不存在");
        }

        // 检查砍价活动是否已过期
        if (bargainActivity.getExpireTime().before(new Date())) {
            bargainActivityMapper.updateStatus(bargainId, "EXPIRED");
            throw new RuntimeException("砍价活动已过期，助力失败");
        }

        // 检查砍价活动状态
        if (!"ACTIVE".equals(bargainActivity.getStatus())) {
            String message = "EXPIRED".equals(bargainActivity.getStatus()) ? "砍价活动已过期" : "砍价活动已结束";
            throw new RuntimeException(message);
        }

        // 检查商品是否还存在且有库存（防止商品被其他人买走）
        Product product = productMapper.selectById(bargainActivity.getProductId());
        if (product == null) {
            bargainActivityMapper.updateStatus(bargainId, "FAILED");
            throw new RuntimeException("商品已下架，助力失败");
        }
        if (product.getStock() < 1) {
            bargainActivityMapper.updateStatus(bargainId, "FAILED");
            throw new RuntimeException("商品已售罄，助力失败");
        }

        // 检查是否已经帮助过（如果有helperId）
        if (helperId != null) {
            BargainHelp existingHelp = bargainHelpMapper.selectByBargainIdAndHelperId(bargainId, helperId);
            if (existingHelp != null) {
                throw new RuntimeException("您已经帮助过该砍价活动");
            }
        }

        // 计算砍价金额
        BigDecimal remainingAmount = bargainActivity.getCurrentPrice().subtract(bargainActivity.getTargetPrice());
        BigDecimal cutAmount;

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("砍价活动已完成");
        }

        // 随机砍价金额：剩余金额的5%-20%
        double ratio = 0.05 + random.nextDouble() * 0.15;
        cutAmount = remainingAmount.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);

        // 确保至少砍0.01元
        if (cutAmount.compareTo(new BigDecimal("0.01")) < 0) {
            cutAmount = new BigDecimal("0.01");
        }

        // 确保不超过剩余金额
        if (cutAmount.compareTo(remainingAmount) > 0) {
            cutAmount = remainingAmount;
        }

        // 创建助力记录
        BargainHelp bargainHelp = new BargainHelp();
        bargainHelp.setBargainId(bargainId);
        bargainHelp.setHelperId(helperId);
        bargainHelp.setHelperName(helperName);
        bargainHelp.setCutAmount(cutAmount);
        bargainHelp.setCreatedAt(new Date());

        bargainHelpMapper.insert(bargainHelp);

        // 更新当前价格
        BigDecimal newPrice = bargainActivity.getCurrentPrice().subtract(cutAmount);
        bargainActivity.setCurrentPrice(newPrice);

        // 检查是否达到目标价格
        if (newPrice.compareTo(bargainActivity.getTargetPrice()) <= 0) {
            bargainActivity.setCurrentPrice(bargainActivity.getTargetPrice());
            bargainActivity.setStatus("SUCCESS");
        }

        bargainActivity.setUpdatedAt(new Date());
        bargainActivityMapper.update(bargainActivity);

        return bargainHelp;
    }

    /**
     * 获取砍价活动详情
     */
    public BargainActivity getBargainActivity(Long bargainId) {
        return bargainActivityMapper.selectById(bargainId);
    }

    /**
     * 获取用户的砍价活动列表
     */
    public List<BargainActivity> getUserBargainActivities(Long userId) {
        return bargainActivityMapper.selectByUserId(userId);
    }

    /**
     * 获取砍价活动的助力记录
     */
    public List<BargainHelp> getBargainHelpList(Long bargainId) {
        return bargainHelpMapper.selectByBargainId(bargainId);
    }

    /**
     * 定时任务：关闭过期的砍价活动
     */
    @Transactional
    public void closeExpiredBargains() {
        List<BargainActivity> expiredActivities = bargainActivityMapper.selectExpiredActivities();
        for (BargainActivity activity : expiredActivities) {
            bargainActivityMapper.updateStatus(activity.getId(), "EXPIRED");
        }
    }

    /**
     * 放弃砍价，以当前价格购买
     */
    @Transactional
    public Map<String, Object> abandonAndBuy(Long bargainId, Long userId) {
        // 获取砍价活动
        BargainActivity bargainActivity = bargainActivityMapper.selectById(bargainId);
        if (bargainActivity == null) {
            throw new RuntimeException("砍价活动不存在");
        }

        // 验证是否是活动发起者
        if (!bargainActivity.getUserId().equals(userId)) {
            throw new RuntimeException("只有活动发起者才能放弃砍价");
        }

        // 检查活动是否已过期
        if (bargainActivity.getExpireTime().before(new Date())) {
            bargainActivityMapper.updateStatus(bargainId, "EXPIRED");
            throw new RuntimeException("砍价活动已过期（超过24小时），无法购买");
        }

        // 验证活动状态
        if (!"ACTIVE".equals(bargainActivity.getStatus())) {
            String message = "EXPIRED".equals(bargainActivity.getStatus()) ?
                "砍价活动已过期" : "砍价活动已结束";
            throw new RuntimeException(message);
        }

        // 检查商品是否还在售（使用行锁防止并发问题）
        Product product = productMapper.selectByIdForUpdate(bargainActivity.getProductId());
        if (product == null) {
            bargainActivityMapper.updateStatus(bargainId, "FAILED");
            throw new RuntimeException("商品已下架");
        }
        if (product.getStock() < 1) {
            bargainActivityMapper.updateStatus(bargainId, "FAILED");
            throw new RuntimeException("商品已售罄，砍价失败");
        }

        // 将砍价活动标记为已放弃（但成功购买）
        bargainActivity.setStatus("ABANDONED");
        bargainActivity.setUpdatedAt(new Date());
        bargainActivityMapper.update(bargainActivity);

        // 创建订单，使用当前砍价价格（此时才扣减库存）
        Map<String, Object> result = createBargainOrder(userId, bargainActivity.getProductId(),
                                                        bargainActivity.getCurrentPrice());

        return result;
    }

    /**
     * 以砍价成功的价格购买
     */
    @Transactional
    public Map<String, Object> buyAtBargainPrice(Long bargainId, Long userId) {
        // 获取砍价活动
        BargainActivity bargainActivity = bargainActivityMapper.selectById(bargainId);
        if (bargainActivity == null) {
            throw new RuntimeException("砍价活动不存在");
        }

        // 验证是否是活动发起者
        if (!bargainActivity.getUserId().equals(userId)) {
            throw new RuntimeException("只有活动发起者才能购买");
        }

        // 检查活动是否已过期（砍价成功后也有24小时期限）
        if (bargainActivity.getExpireTime().before(new Date())) {
            bargainActivityMapper.updateStatus(bargainId, "EXPIRED");
            throw new RuntimeException("砍价活动已过期（超过24小时），无法购买");
        }

        // 验证活动状态必须是成功
        if (!"SUCCESS".equals(bargainActivity.getStatus())) {
            if ("EXPIRED".equals(bargainActivity.getStatus())) {
                throw new RuntimeException("砍价活动已过期，无法购买");
            } else if ("COMPLETED".equals(bargainActivity.getStatus())) {
                throw new RuntimeException("该砍价订单已经购买过了");
            } else if ("FAILED".equals(bargainActivity.getStatus())) {
                throw new RuntimeException("砍价活动已失败");
            }
            throw new RuntimeException("砍价活动未成功，不能购买");
        }

        // 检查商品是否还在售（使用行锁防止并发问题）
        Product product = productMapper.selectByIdForUpdate(bargainActivity.getProductId());
        if (product == null) {
            bargainActivityMapper.updateStatus(bargainId, "FAILED");
            throw new RuntimeException("商品已下架，砍价失败");
        }
        if (product.getStock() < 1) {
            // 商品被其他人买走了，标记砍价失败
            bargainActivityMapper.updateStatus(bargainId, "FAILED");
            throw new RuntimeException("商品已售罄，砍价失败");
        }

        // 将砍价活动标记为已使用
        bargainActivity.setStatus("COMPLETED");
        bargainActivity.setUpdatedAt(new Date());
        bargainActivityMapper.update(bargainActivity);

        // 创建订单，使用目标价格（砍价成功后的价格）（此时才扣减库存）
        Map<String, Object> result = createBargainOrder(userId, bargainActivity.getProductId(),
                                                        bargainActivity.getTargetPrice());

        return result;
    }

    /**
     * 创建砍价订单的辅助方法
     */
    private Map<String, Object> createBargainOrder(Long userId, Long productId, BigDecimal price) {
        // 调用订单服务创建订单，使用自定义价格
        lut.cn.c2cplatform.entity.Order order = orderService.createBargainOrder(
            userId.intValue(),
            productId,
            1,
            price
        );

        // 返回订单信息
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("userId", order.getUserId());
        result.put("totalAmount", order.getTotalAmount());
        result.put("status", order.getStatus());
        result.put("createdAt", order.getCreatedAt());

        return result;
    }
}

