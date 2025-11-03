package lut.cn.c2cplatform.service.impl;

import lut.cn.c2cplatform.dto.CreditScoreResponse;
import lut.cn.c2cplatform.entity.CreditScore;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.CreditScoreMapper;
import lut.cn.c2cplatform.mapper.OrderMapper;
import lut.cn.c2cplatform.mapper.ReviewMapper;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.service.CreditScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreditScoreServiceImpl implements CreditScoreService {

    @Autowired
    private CreditScoreMapper creditScoreMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public CreditScoreResponse getUserCreditScore(Long userId) {
        CreditScore creditScore = creditScoreMapper.selectByUserId(userId);

        // 如果不存在，初始化
        if (creditScore == null) {
            initializeCreditScore(userId);
            creditScore = creditScoreMapper.selectByUserId(userId);
        }

        User user = userMapper.selectById(userId);

        CreditScoreResponse response = new CreditScoreResponse();
        response.setUserId(userId);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setDisplayName(user.getDisplayName());
            response.setAvatarUrl(user.getAvatarUrl());
        }

        response.setTotalScore(creditScore.getTotalScore());
        response.setLevel(creditScore.getLevel());
        response.setLevelName(getLevelName(creditScore.getLevel()));
        response.setTotalSales(creditScore.getTotalSales());
        response.setTotalPurchases(creditScore.getTotalPurchases());
        response.setAverageSellerRating(creditScore.getAverageSellerRating());
        response.setPositiveReviews(creditScore.getPositiveReviews());
        response.setNeutralReviews(creditScore.getNeutralReviews());
        response.setNegativeReviews(creditScore.getNegativeReviews());

        Integer totalReviews = creditScore.getPositiveReviews() +
                              creditScore.getNeutralReviews() +
                              creditScore.getNegativeReviews();
        response.setTotalReviews(totalReviews);

        // 计算好评率
        if (totalReviews > 0) {
            response.setPositiveRate((double) creditScore.getPositiveReviews() / totalReviews * 100);
        } else {
            response.setPositiveRate(0.0);
        }

        return response;
    }

    @Override
    @Transactional
    public void updateCreditScore(Long userId) {
        CreditScore creditScore = creditScoreMapper.selectByUserId(userId);

        if (creditScore == null) {
            initializeCreditScore(userId);
            creditScore = creditScoreMapper.selectByUserId(userId);
        }

        // 计算总销售数（作为卖家完成的订单数）
        Integer totalSales = orderMapper.countCompletedOrdersBySeller(userId.intValue());

        // 计算总购买数（作为买家完成的订单数）
        Integer totalPurchases = orderMapper.countCompletedOrdersByBuyer(userId.intValue());

        // 计算平均卖家评分
        Double averageSellerRating = reviewMapper.getAverageSellerRating(userId.intValue());
        if (averageSellerRating == null) {
            averageSellerRating = 0.0;
        }

        // 计算好评、中评、差评数
        Integer positiveReviews = reviewMapper.countPositiveReviews(userId.intValue());
        Integer neutralReviews = reviewMapper.countNeutralReviews(userId.intValue());
        Integer negativeReviews = reviewMapper.countNegativeReviews(userId.intValue());

        // 计算总信用分
        // 算法：基础分100 + 完成交易数*2 + 平均评分*20 - 差评数*10
        int baseScore = 100;
        int transactionScore = (totalSales + totalPurchases) * 2;
        int ratingScore = (int) (averageSellerRating * 20);
        int penaltyScore = negativeReviews * 10;

        int totalScore = baseScore + transactionScore + ratingScore - penaltyScore;
        totalScore = Math.max(0, totalScore); // 确保不为负数

        // 计算信用等级（1-5级）
        int level = calculateLevel(totalScore, totalSales + totalPurchases, averageSellerRating);

        // 更新信用分
        creditScore.setTotalScore(totalScore);
        creditScore.setLevel(level);
        creditScore.setTotalSales(totalSales);
        creditScore.setTotalPurchases(totalPurchases);
        creditScore.setAverageSellerRating(averageSellerRating);
        creditScore.setPositiveReviews(positiveReviews);
        creditScore.setNeutralReviews(neutralReviews);
        creditScore.setNegativeReviews(negativeReviews);
        creditScore.setUpdatedAt(Instant.now());

        creditScoreMapper.update(creditScore);
    }

    @Override
    @Transactional
    public void initializeCreditScore(Long userId) {
        int exists = creditScoreMapper.existsByUserId(userId);
        if (exists > 0) {
            return; // 已存在，不需要初始化
        }

        CreditScore creditScore = new CreditScore();
        creditScore.setUserId(userId);
        creditScore.setTotalScore(100); // 初始分100
        creditScore.setLevel(1); // 初始等级1
        creditScore.setTotalSales(0);
        creditScore.setTotalPurchases(0);
        creditScore.setAverageSellerRating(0.0);
        creditScore.setPositiveReviews(0);
        creditScore.setNeutralReviews(0);
        creditScore.setNegativeReviews(0);
        creditScore.setUpdatedAt(Instant.now());

        creditScoreMapper.insert(creditScore);
    }

    /**
     * 计算信用等级
     * 等级1: 新手（总分<150 或 交易数<5）
     * 等级2: 铜牌（总分>=150 且 交易数>=5）
     * 等级3: 银牌（总分>=250 且 交易数>=20 且 平均评分>=3.5）
     * 等级4: 金牌（总分>=400 且 交易数>=50 且 平均评分>=4.0）
     * 等级5: 钻石（总分>=600 且 交易数>=100 且 平均评分>=4.5）
     */
    private int calculateLevel(int totalScore, int totalTransactions, double avgRating) {
        if (totalScore >= 600 && totalTransactions >= 100 && avgRating >= 4.5) {
            return 5; // 钻石
        } else if (totalScore >= 400 && totalTransactions >= 50 && avgRating >= 4.0) {
            return 4; // 金牌
        } else if (totalScore >= 250 && totalTransactions >= 20 && avgRating >= 3.5) {
            return 3; // 银牌
        } else if (totalScore >= 150 && totalTransactions >= 5) {
            return 2; // 铜牌
        } else {
            return 1; // 新手
        }
    }

    /**
     * 获取等级名称
     */
    private String getLevelName(int level) {
        switch (level) {
            case 1:
                return "新手";
            case 2:
                return "铜牌卖家";
            case 3:
                return "银牌卖家";
            case 4:
                return "金牌卖家";
            case 5:
                return "钻石卖家";
            default:
                return "未知";
        }
    }
}

