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

    private static final String express = "shentong,shunfeng";

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
        // 查询邮费
        Integer areaId = 0;
        for (ConsigneeVo consigneeVo : consigneeVos) {
            if (consigneeVo.getStatus() == 1) {
                areaId = consigneeVo.getAreaId();
                break;
            } // 默认收货地址
        }
        // TODO 根据区ID查询省ID
        List<Double> doubleList = calcFreight(areaId, weight, express);
        for (int i = 0; i < confirmOrderVo.getExpress().size(); i++) {
            if (i == 2) {
                // 德邦物流，商品件数大于50件或者重量大于999，运费:0，否则走申通运费。
                double freight = confirmOrderVo.getGoodsPieces() >= 50 || weight > 999 ? 0 : doubleList.get(0);
                confirmOrderVo.getExpress().get(i).setFreight(freight);
            } else {
                confirmOrderVo.getExpress().get(i).setFreight(doubleList.get(i));
            }
        }
        // 5. 账户余额
        Long remain = axxBankService.getRemainAidouByInsId(insId);
        confirmOrderVo.setBalance(remain / 10000);
        confirmOrderVo.setStatus(confirmOrderVo.getBalance() >= goodsAmount ? 0 : 1);
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
     * @param provinceId 省ID
     * @param weight     商品重量
     * @param express    快递公司，多个,分割
     * @return
     */
    private List<Double> calcFreight(Integer provinceId, Double weight, String express) {
        List<Double> doubleList = Lists.newArrayList();
        ResultData<List<HashMap<String, Object>>> resultData = goodsService.caleFreight(provinceId, weight, express);
        List<HashMap<String, Object>> listMap = resultData.getData();
        for (HashMap<String, Object> stringObjectHashMap : listMap) {
            doubleList.add(Double.valueOf(stringObjectHashMap.get("totalFreight").toString()));
        }
        return doubleList;
    }

    /**
     * 修改商品数量，选择部分商品，重新选择收货人，重新计算运费。
     *
     * @param userId
     * @param consigneeId
     * @param goodsTypeIds
     * @return
     */
    public List<ConfirmExpressVo> reloadFreight(Integer userId, Integer consigneeId, List<Integer> goodsTypeIds) {
        List<ConfirmExpressVo> confirmExpressVos = expressUtil.getExpress();
        List<ShoppingCartList> shoppingCartLists = null;
        if (CollectionUtils.isEmpty(goodsTypeIds)) {
            shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId, goodsTypeIds);
        } else {
            shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId);
        }
        goodsTypeIds = Lists.newArrayList();
        int goodsPieces = 0; // 商品总件数
        // 数量
        Map<Integer, Integer> goodsNum = Maps.newHashMap();
        for (ShoppingCartList shoppingCartList : shoppingCartLists) {
            goodsTypeIds.add(shoppingCartList.getGoodsTypeId());
            goodsNum.put(shoppingCartList.getGoodsTypeId(), shoppingCartList.getNum());
            goodsPieces += shoppingCartList.getNum();
        }
        double weight = 0; // 重量
        List<ConfirmGoodsVo> goodsVos = vGoodsService.queryGoodsInfo(goodsTypeIds);
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            // 数量*单重量
            weight += goodsNum.get(goodsVo.getGoodsTypeId()) * goodsVo.getWeight();
        }
        // 查询收货信息
        Consignee consignee = consigneeService.selectById(consigneeId);
        // TODO 根据区ID查询省ID
        List<Double> doubleList = calcFreight(consignee.getAreaId(), weight, express);
        for (int i = 0; i < confirmExpressVos.size(); i++) {
            if (i == 2) {
                // 德邦物流，商品件数大于50件或者重量大于999，运费:0，否则走申通运费。
                double freight = goodsPieces >= 50 || weight > 999 ? 0 : doubleList.get(0);
                confirmExpressVos.get(i).setFreight(freight);
            } else {
                confirmExpressVos.get(i).setFreight(doubleList.get(i));
            }
        }
        return confirmExpressVos;
    }
}
