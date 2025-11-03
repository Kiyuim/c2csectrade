package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.dto.CreditScoreResponse;
import lut.cn.c2cplatform.entity.CreditScore;

public interface CreditScoreService {

    /**
     * 获取用户信用分
     */
    CreditScoreResponse getUserCreditScore(Long userId);

    /**
     * 更新用户信用分（在评价、交易完成时调用）
     */
    void updateCreditScore(Long userId);

    /**
     * 初始化用户信用分
     */
    void initializeCreditScore(Long userId);
}

