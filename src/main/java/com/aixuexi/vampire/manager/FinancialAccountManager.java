package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.util.Functions;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.BusinessResult;
import com.gaosi.api.axxBank.model.CostProxyParams;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.ChangeCostProxyHandler;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.revolver.constant.PayTypeConstant;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 查询财务的余额的服务
 *
 * @author baopan
 * @createTime 20180205
 */
@Service("financialAccountManager")
public class FinancialAccountManager {

    private static final Logger log = LoggerFactory.getLogger(FinancialAccountManager.class);

    @Resource
    private FinancialAccountService financialAccountService;

    /**
     * 获取机构的账户信息
     *
     * @param insId 机构
     * @return 账户信息
     */
    public RemainResult getAccountInfoByInsId(Integer insId) {
        RemainResult rr = financialAccountService.getRemainByInsId(insId);
        if (rr == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        return rr;
    }

    /**
     * 检查余额是否不足
     *
     * @param rr           RemainResult
     * @param consumeMoney consumeMoney
     */
    public void checkRemainMoney(RemainResult rr, Long consumeMoney) {
        Long remain = rr.getUsableRemain();
        if (consumeMoney > remain) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "余额不足");
        }
    }

    /**
     * 订单支付
     *
     * @param orderId
     * @return
     */
    public void pay(final String orderId, String token, Integer categoryId, Double amount) {
        // 显示文案
        String amountStr = String.format("%.2f", amount);
        StringBuilder optionDesc = new StringBuilder();
        optionDesc.append("支付订单，商城订单号").append(orderId)
                .append("，实付").append(amountStr).append("爱豆。");
        amount = AmountUtil.multiply(amount, 10000);//扩大10000倍
        //查询当前机构账号余额,检查余额是否充足
        RemainResult rr = getAccountInfoByInsId(UserHandleUtil.getInsId());
        checkRemainMoney(rr, amount.longValue());
        ChangeCostProxyHandler proxyHandler = new ChangeCostProxyHandler(financialAccountService);
        CostProxyParams proxyParams = new CostProxyParams();
        proxyParams.setInsId(UserHandleUtil.getInsId());
        proxyParams.setAmount(amount.longValue());
        proxyParams.setDiscount(100);
        proxyParams.setOperatorId(UserHandleUtil.getUserId());
        proxyParams.setOperatorType(1);
        proxyParams.setOptionItemEnum(PayTypeConstant.PayType.getOptionItemEnum(categoryId));
        proxyParams.setToken(token);
        proxyParams.setOptionDesc(optionDesc.toString());
        BusinessResult businessResult = proxyHandler.costAidou(proxyParams, new Functions.Function0<BusinessResult>() {
            @Override
            public BusinessResult apply() {
                return new BusinessResult(orderId, null);
            }
        });
        if (businessResult.getCgFinancialResult().getStatus() == 1) {
            log.info("订单扣费成功，optionDesc：{},amount:{},token:{}", optionDesc, amountStr, token);
        } else {
            throw new BusinessException(ExceptionCode.UNKNOWN, "订单扣费失败，错误信息：" +
                    businessResult.getCgFinancialResult().getMessage());
        }

    }

}
