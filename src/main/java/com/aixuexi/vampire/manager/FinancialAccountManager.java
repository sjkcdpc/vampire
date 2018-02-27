package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
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

    @Resource
    private FinancialAccountService finAccService;

    /**
     * 获取机构的账户信息
     *
     * @param insId 机构
     * @return 账户信息
     */
    public RemainResult getAccountInfoByInsId(Integer insId) {
        RemainResult rr = finAccService.getRemainByInsId(insId);
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
}
