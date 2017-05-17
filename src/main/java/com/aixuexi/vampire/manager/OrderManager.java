package com.aixuexi.vampire.manager;

import com.aixuexi.account.api.AxxBankService;
import com.aixuexi.account.api.GoodsService;
import com.aixuexi.ordernew.facade.OrderServiceFacade;
import com.aixuexi.vampire.util.ExpressUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.independenceDay.entity.ShoppingCartList;
import com.gaosi.api.independenceDay.model.Consignee;
import com.gaosi.api.independenceDay.service.ConsigneeService;
import com.gaosi.api.independenceDay.service.ShoppingCartService;
import com.gaosi.api.independenceDay.vo.ConfirmExpressVo;
import com.gaosi.api.independenceDay.vo.ConfirmGoodsVo;
import com.gaosi.api.independenceDay.vo.ConfirmOrderVo;
import com.gaosi.api.independenceDay.vo.ConsigneeVo;
import com.gaosi.api.independenceDay.vo.FreightVo;
import com.gaosi.util.model.ResultData;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private GoodsService goodsService;

    @Resource(name = "vGoodsService")
    private com.gaosi.api.revolver.GoodsService vGoodsService;

    @Autowired
    private OrderServiceFacade orderServiceFacade;

    @Autowired
    private ExpressUtil expressUtil;

    private static final String express = "shunfeng,shentong";

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
        // 3. 用户购物车中商品清单
        List<ShoppingCartList> shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId);
        List<Integer> goodsTypeIds = Lists.newArrayList();
        int goodsPieces = 0; // 商品总件数
        double goodsAmount = 0; // 总金额
        double weight = 0; // 商品重量
        // 数量 goodsTypeIds -> num
        Map<Integer, Integer> goodsNum = Maps.newHashMap();
        for (ShoppingCartList shoppingCartList : shoppingCartLists) {
            goodsTypeIds.add(shoppingCartList.getGoodsTypeId());
            goodsNum.put(shoppingCartList.getGoodsTypeId(), shoppingCartList.getNum());
            goodsPieces += shoppingCartList.getNum();
        }
        // 4. 根据goodsTypeIds查询商品其他信息
        List<ConfirmGoodsVo> goodsVos = vGoodsService.queryGoodsInfo(goodsTypeIds);
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            goodsVo.setNum(goodsNum.get(goodsVo.getGoodsTypeId()));
            // 数量*单价
            goodsVo.setTotal(goodsVo.getNum() * goodsVo.getPrice());
            // 数量*单重量
            weight += goodsVo.getNum() * goodsVo.getWeight();
            goodsAmount += goodsVo.getTotal();
        }
        confirmOrderVo.setGoodsItem(goodsVos);
        confirmOrderVo.setGoodsPieces(goodsPieces);
        confirmOrderVo.setGoodsAmount(goodsAmount);
        // 计算邮费
        Integer provinceId = 0;
        for (ConsigneeVo consigneeVo : consigneeVos) {
            if (consigneeVo.getStatus() == 1) {
                provinceId = consigneeVo.getProvinceId();
                break;
            } // 默认收货地址
        }
        calcFreight(provinceId, weight, confirmOrderVo.getExpress());
        // 5. 账户余额
        Long remain = axxBankService.getRemainAidouByInsId(insId);
        confirmOrderVo.setBalance(remain / 10000);
        return confirmOrderVo;
    }

    /**
     * 提交订单
     *
     * @param userId
     * @param consigneeId
     * @param receivePhone
     * @param express
     * @param goodsTypeIds
     * @return
     */
    public Object submit(Integer userId, Integer consigneeId, String receivePhone, String express, List<Integer> goodsTypeIds) {

        return null;
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

    /**
     * 计算运费
     *
     * @param provinceId     省ID
     * @param weight         商品重量
     * @param expressVoLists 物流信息
     */
    private void calcFreight(Integer provinceId, double weight, List<ConfirmExpressVo> expressVoLists) {
        ResultData<List<HashMap<String, Object>>> resultData = goodsService.caleFreight(provinceId, weight, express);
        List<HashMap<String, Object>> listMap = resultData.getData();
        for (int i = 0; i < expressVoLists.size(); i++) {
            ConfirmExpressVo confirmExpressVo = expressVoLists.get(i);
            HashMap<String, Object> map = null;
            if (i == 2) {
                map = listMap.get(1);
            } else {
                map = listMap.get(i);
            }
            confirmExpressVo.setFirstFreight(map.get("firstFreight").toString());
            confirmExpressVo.setBeyondPrice(map.get("beyondPrice").toString());
            confirmExpressVo.setBeyondWeight(map.get("beyondWeight").toString());
            confirmExpressVo.setTotalFreight(map.get("totalFreight").toString());
        }

    }

    /**
     * 修改商品数量，选择部分商品，重新选择收货人，重新计算运费。
     *
     * @param userId       用户ID
     * @param insId        机构ID
     * @param provinceId   省ID
     * @param goodsTypeIds 商品类型ID
     * @return
     */
    public FreightVo reloadFreight(Integer userId, Integer insId, Integer provinceId, List<Integer> goodsTypeIds) {
        FreightVo freightVo = new FreightVo();
        List<ConfirmExpressVo> confirmExpressVos = expressUtil.getExpress();
        List<ShoppingCartList> shoppingCartLists = null;
        if (CollectionUtils.isEmpty(goodsTypeIds)) {
            shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId, goodsTypeIds);
        } else {
            shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId);
        }
        goodsTypeIds = Lists.newArrayList();
        int goodsPieces = 0; // 商品总件数
        // 数量 goodsTypeIds - > num
        Map<Integer, Integer> goodsNum = Maps.newHashMap();
        for (ShoppingCartList shoppingCartList : shoppingCartLists) {
            goodsTypeIds.add(shoppingCartList.getGoodsTypeId());
            goodsNum.put(shoppingCartList.getGoodsTypeId(), shoppingCartList.getNum());
            goodsPieces += shoppingCartList.getNum();
        }
        double weight = 0; // 重量
        double goodsAmount = 0; // 总金额
        List<ConfirmGoodsVo> goodsVos = vGoodsService.queryGoodsInfo(goodsTypeIds);
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            // 数量*单重量
            weight += goodsNum.get(goodsVo.getGoodsTypeId()) * goodsVo.getWeight();
            // 数量*单价
            goodsAmount += goodsNum.get(goodsVo.getGoodsTypeId()) * goodsVo.getPrice();
        }
        // 计算邮费
        calcFreight(provinceId, weight, confirmExpressVos);
        // set
        freightVo.setGoodsPieces(goodsPieces);
        freightVo.setGoodsWeight(weight);
        freightVo.setGoodsAmount(goodsAmount);
        freightVo.setExpress(confirmExpressVos);
        // 账号余额
        Long remain = axxBankService.getRemainAidouByInsId(insId);
        freightVo.setBalance(remain / 10000);
        return freightVo;
    }
}
