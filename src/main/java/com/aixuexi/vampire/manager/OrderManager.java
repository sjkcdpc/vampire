package com.aixuexi.vampire.manager;

import com.aixuexi.account.api.AxxBankService;
import com.aixuexi.account.api.GoodsRestService;
import com.aixuexi.ordernew.facade.OrderServiceFacade;
import com.aixuexi.vampire.util.ExpressUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.independenceDay.model.Consignee;
import com.gaosi.api.independenceDay.service.ConsigneeService;
import com.gaosi.api.independenceDay.service.ShoppingCartService;
import com.gaosi.api.independenceDay.vo.ConfirmOrderVo;
import com.gaosi.api.independenceDay.vo.ConsigneeVo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单
 * Created by gaoxinzhong on 2017/5/15.
 */
@Service("orderManager")
public class OrderManager {

    @Autowired
    private ConsigneeService consigneeService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private AxxBankService axxBankService;

    @Autowired
    private AreaApi areaApi;

    @Autowired
    private GoodsRestService goodsRestService;

    @Autowired
    private OrderServiceFacade orderServiceFacade;

    @Autowired
    private ExpressUtil expressUtil;

    /**
     * 核对订单信息
     *
     * @param userId
     * @param insId
     * @return
     */
    public ConfirmOrderVo confirmOrder(Integer userId, Integer insId) {
        ConfirmOrderVo confirmOrderVo = new ConfirmOrderVo();
        // 1. 收货人地址
        List<ConsigneeVo> consigneeVos = findConsignee(insId);
        confirmOrderVo.setConsignees(consigneeVos);
        // 2. 快递公司
        confirmOrderVo.setExpress(expressUtil.getExpress());
        // 3. 商品清单
        // shoppingCartService.queryShoppingCartDetail(userId);
        // TODO
        // 5. 商品件数，商品总金额，邮费
        // 6. 账户余额
        Long remain = axxBankService.getRemainAidouByInsId(insId);
        confirmOrderVo.setBalance(remain / 10000);
        return confirmOrderVo;
    }

    /**
     * 根据机构ID查询收货地址
     *
     * @param insId 机构ID
     * @return
     */
    private List<ConsigneeVo> findConsignee(Integer insId) {
        List<Consignee> consignees = consigneeService.selectByIns(insId, null);
        if (CollectionUtils.isNotEmpty(consignees)) {
            String consigneeJson = JSONObject.toJSONString(consignees);
            List<ConsigneeVo> consigneeVos = JSONObject.parseArray(consigneeJson, ConsigneeVo.class);
            // TODO 批量查询省市区 areaApi
            return consigneeVos;
        }
        return Lists.newArrayList();
    }
}
