package com.aixuexi.vampire.manager;

import com.aixuexi.account.api.AxxBankService;
import com.aixuexi.account.api.GoodsService;
import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.vampire.util.ExpressUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.independenceDay.entity.ShoppingCartList;
import com.gaosi.api.independenceDay.service.ShoppingCartService;
import com.gaosi.api.independenceDay.vo.OrderSuccessVo;
import com.gaosi.api.revolver.facade.ConsigneeServiceFacade;
import com.gaosi.api.revolver.facade.GoodsServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.Consignee;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.model.OrderDetail;
import com.gaosi.api.revolver.vo.ConfirmExpressVo;
import com.gaosi.api.revolver.vo.ConfirmGoodsVo;
import com.gaosi.api.revolver.vo.ConfirmOrderVo;
import com.gaosi.api.revolver.vo.ConsigneeVo;
import com.gaosi.api.revolver.vo.FreightVo;
import com.gaosi.util.model.ResultData;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private ConsigneeServiceFacade consigneeServiceFacade;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private AxxBankService axxBankService;

    @Autowired
    private AreaApi areaApi;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsServiceFacade goodsServiceFacade;

    @Autowired
    private OrderServiceFacade orderServiceFacade;

    @Autowired
    private ExpressUtil expressUtil;

    private static final String express = "shunfeng,shentong";

    private static final String express_dbwl = "debangwuliu";

    private static final String express_shentong = "shentong";

    private static final String express_shunfeng = "shunfeng";

    private List<Integer> insIds = Lists.newArrayList(25, 26);

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 核对订单信息
     *
     * @param userId 用户ID
     * @param insId  机构ID
     * @return
     */
    public ConfirmOrderVo confirmOrder(Integer userId, Integer insId) {
        logger.info("confirmOrder start --> userId : {}, insId : {}", userId, insId);
        ConfirmOrderVo confirmOrderVo = new ConfirmOrderVo();
        // 1. 收货人地址
        List<ConsigneeVo> consigneeVos = findConsignee(insId);
        confirmOrderVo.setConsignees(consigneeVos);
        // 2. 快递公司
        confirmOrderVo.setExpress(expressUtil.getExpress());
        // 3. 用户购物车中商品清单
        List<ShoppingCartList> shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId);
        if (CollectionUtils.isEmpty(shoppingCartLists)) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
        }
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
        List<ConfirmGoodsVo> goodsVos = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            goodsVo.setNum(goodsNum.get(goodsVo.getGoodsTypeId()));
            // 数量*单价
            goodsVo.setTotal(goodsVo.getNum() * goodsVo.getPrice());
            if (goodsVo.getStatus() == 1) { // 上架
                // 数量*单重量
                weight += goodsVo.getNum() * goodsVo.getWeight();
                goodsAmount += goodsVo.getTotal();
            } else { // 下架
                goodsPieces -= goodsVo.getNum();
            }
        }
        confirmOrderVo.setGoodsItem(goodsVos);
        confirmOrderVo.setGoodsPieces(goodsPieces);
        confirmOrderVo.setGoodsAmount(goodsAmount);
        // 计算邮费
        // Integer provinceId = 0;
        // if (CollectionUtils.isNotEmpty(consigneeVos)) provinceId = consigneeVos.get(0).getProvinceId();
        // calcFreight(provinceId, weight, confirmOrderVo.getExpress());
        // 5. 账户余额
        Long remain = axxBankService.getRemainAidouByInsId(insId);
        confirmOrderVo.setBalance(Double.valueOf(remain) / 100000);
        logger.info("confirmOrder end --> confirmOrderVo : {}", confirmOrderVo);
        return confirmOrderVo;
    }

    /**
     * 提交订单
     *
     * @param userId       用户ID
     * @param insId        机构ID
     * @param consigneeId  收货人ID
     * @param receivePhone 收货通知手机号
     * @param express      快递
     * @param goodsTypeIds 商品类型ID
     * @return
     */
    public OrderSuccessVo submit(Integer userId, Integer insId, Integer consigneeId, String receivePhone,
                                 String express, List<Integer> goodsTypeIds) {
        logger.info("submitOrder --> userId : {}, insId : {}, consigneeId : {}, receivePhone : {}, express : {}, goodsTypeIds : {}",
                userId, insId, consigneeId, receivePhone, express, goodsTypeIds);
        List<ShoppingCartList> shoppingCartLists = null;
        if (CollectionUtils.isEmpty(goodsTypeIds)) {
            shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId, goodsTypeIds);
        } else {
            shoppingCartLists = shoppingCartService.queryShoppingCartDetail(userId);
        }
        if (CollectionUtils.isEmpty(shoppingCartLists)) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
        }
        // 是否走发网
        Boolean syncToWms = true;
        if (insIds.contains(insId) || expressUtil.getSyncToWms()) {
            syncToWms = false;
        } // 机构25，26或测试环境不走发网
        logger.info("submitOrder --> syncToWms : {}", syncToWms);
        // 创建订单对象
        GoodsOrder goodsOrder = createGoodsOrder(shoppingCartLists, userId, insId, consigneeId, receivePhone, express);
        logger.info("submitOrder --> goodsOrder info : {}", JSONObject.toJSONString(goodsOrder));
        // 支付金额 = 商品金额 + 邮费
        Double amount = (goodsOrder.getConsumeAmount() + goodsOrder.getFreight()) * 10000;
        // 账号余额
        Long remain = axxBankService.getRemainAidouByInsId(insId);
        if (amount.longValue() > remain) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "余额不足");
        }
        // 创建订单
        ApiResponse<String> apiResponse = orderServiceFacade.createOrder(goodsOrder, syncToWms);
        if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
            logger.info("submitOrder --> orderId : {}", apiResponse.getBody());
            try {
                List<Integer> shoppingCartListIds = Lists.newArrayList();
                for (ShoppingCartList shoppingCartList : shoppingCartLists) {
                    shoppingCartListIds.add(shoppingCartList.getId());
                }
                shoppingCartService.clearShoppingCart(shoppingCartListIds);
            } catch (Throwable e) {
                logger.error("submitOrder --> clearShoppingCart fail, orderId : {}", apiResponse.getBody());
            }
            return new OrderSuccessVo(apiResponse.getBody(), findTipsByExpress(express));
        } else {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "订单创建失败");
        }
    }

    /**
     * 创建订单对象
     *
     * @param shoppingCartLists 购物车中的商品
     * @param userId            用户ID
     * @param insId             机构ID
     * @param consigneeId       收货人ID
     * @param receivePhone      收货通知手机号
     * @param express           快递
     * @return
     */
    private GoodsOrder createGoodsOrder(List<ShoppingCartList> shoppingCartLists, Integer userId, Integer insId,
                                        Integer consigneeId, String receivePhone, String express) {
        // 订单
        GoodsOrder goodsOrder = new GoodsOrder();
        // 商品类型ID
        List<Integer> goodsTypeIds = Lists.newArrayList();
        // 商品件数
        int goodsPieces = 0;
        // 商品重量
        double weight = 0;
        // 商品总金额
        double goodsAmount = 0;
        // 商品数量
        Map<Integer, Integer> goodsNum = Maps.newHashMap();
        for (ShoppingCartList shoppingCartList : shoppingCartLists) {
            goodsTypeIds.add(shoppingCartList.getGoodsTypeId());
            goodsNum.put(shoppingCartList.getGoodsTypeId(), shoppingCartList.getNum());
            goodsPieces += shoppingCartList.getNum();
        }
        // 订单详情
        List<OrderDetail> orderDetails = Lists.newArrayList();
        // 查询商品明细
        List<ConfirmGoodsVo> confirmGoodsVos = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            int num = goodsNum.get(confirmGoodsVo.getGoodsTypeId());
            // 数量*单重量
            weight += num * confirmGoodsVo.getWeight();
            // 数量*单价
            goodsAmount += num * confirmGoodsVo.getPrice();
            // 商品明细
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setBarCode(confirmGoodsVo.getBarCode());
            orderDetail.setGoodsId(confirmGoodsVo.getGoodsId());
            orderDetail.setGoodTypeId(confirmGoodsVo.getGoodsTypeId());
            orderDetail.setName(confirmGoodsVo.getGoodsName());
            orderDetail.setNum(num);
            orderDetail.setPrice(confirmGoodsVo.getPrice());
            orderDetails.add(orderDetail);
        }
        goodsOrder.setOrderDetails(orderDetails);
        // 收货人信息
        Consignee consignee = consigneeServiceFacade.selectById(consigneeId);
        goodsOrder.setAreaId(consignee.getAreaId());
        goodsOrder.setConsigneeName(consignee.getName());
        goodsOrder.setConsigneePhone(consignee.getPhone());
        goodsOrder.setConsigneeAddress(consignee.getAddress());
        goodsOrder.setConsumeAmount(goodsAmount); // 商品总金额
        goodsOrder.setInstitutionId(insId);
        goodsOrder.setRemark(StringUtils.EMPTY);
        goodsOrder.setReceivePhone(receivePhone); // 发货通知手机号
        goodsOrder.setUserId(userId);
        boolean isFree = false; // 是否免物流费
        if (express.equals(express_dbwl)) {
            if (weight > 999) {
                goodsOrder.setExpressCode(express_dbwl);
                isFree = true;
            } else if (goodsPieces >= 50) {
                goodsOrder.setExpressCode(express_shentong);
                isFree = true;
            }
        } else {
            goodsOrder.setExpressCode(express);
        }
        // 是否计算邮费
        if (isFree) {
            // 选择德邦物流，weight > 999 || goodsPieces >= 50， 邮费0。
            goodsOrder.setFreight(0D);
        } else {
            List<AddressDTO> addressDTOS = findAddressByIds(consignee.getAreaId());
            // 省ID
            Integer provinceId = addressDTOS.get(0).getProvinceId();
            // 计算邮费
            ResultData<List<HashMap<String, Object>>> resultData = goodsService.caleFreight(provinceId, weight,
                    express.equals(express_dbwl) ? express_shentong : express);
            logger.info("submitOrder --> freight : {}", resultData);
            HashMap<String, Object> freightMap = resultData.getData().get(0);
            goodsOrder.setFreight(Double.valueOf(freightMap.get("totalFreight").toString()));
        }
        return goodsOrder;
    }

    /**
     * 根据机构ID查询收货地址
     *
     * @param insId 机构ID
     * @return
     */
    private List<ConsigneeVo> findConsignee(Integer insId) {
        List<Consignee> consignees = consigneeServiceFacade.selectByIns(insId);
        if (CollectionUtils.isNotEmpty(consignees)) {
            String consigneeJson = JSONObject.toJSONString(consignees);
            List<ConsigneeVo> consigneeVos = JSONObject.parseArray(consigneeJson, ConsigneeVo.class);
            // 区ids
            Integer[] areaIds = new Integer[consigneeVos.size()];
            for (int i = 0; i < consigneeVos.size(); i++) {
                areaIds[i] = consigneeVos.get(i).getAreaId();
            }
            List<AddressDTO> addressDTOS = findAddressByIds(areaIds);
            Map<Integer, AddressDTO> addressMap = Maps.newHashMap();
            for (AddressDTO addressDTO : addressDTOS) {
                addressMap.put(addressDTO.getDistrictId(), addressDTO);
            }
            for (ConsigneeVo consigneeVo : consigneeVos) {
                AddressDTO addressDTO = addressMap.get(consigneeVo.getAreaId());
                consigneeVo.setProvinceId(addressDTO.getProvinceId());
                consigneeVo.setProvince(addressDTO.getProvince());
                consigneeVo.setCityId(addressDTO.getCityId());
                consigneeVo.setCity(addressDTO.getCity());
                consigneeVo.setArea(addressDTO.getDistrict());
            }
            return consigneeVos;
        }
        return Lists.newArrayList();
    }

    /**
     * 批量查询省市区
     *
     * @param ids 区ID
     * @return
     */
    private List<AddressDTO> findAddressByIds(Integer... ids) {
        ApiResponse<List<AddressDTO>> apiResponse = areaApi.findAddressByIds(ids);
        if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
            if (CollectionUtils.isEmpty(apiResponse.getBody())) {
                throw new IllegalArgException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
            }
            return apiResponse.getBody();
        } else {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
        }
    }

    /**
     * 计算运费
     *
     * @param provinceId     省ID
     * @param weight         商品重量
     * @param expressVoLists 物流信息
     */
    private void calcFreight(Integer provinceId, double weight, List<ConfirmExpressVo> expressVoLists) {
        logger.info("calcFreight --> provinceId : {}, weight : {}, expressLists : {}", provinceId, weight, expressVoLists);
        ResultData<List<HashMap<String, Object>>> resultData = goodsService.caleFreight(provinceId, weight, express);
        logger.info("calcFreight --> freight : {}", resultData);
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
        if (CollectionUtils.isEmpty(shoppingCartLists)) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
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
        List<ConfirmGoodsVo> goodsVos = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            if (goodsVo.getStatus() == 1) { // 上架
                // 数量*单重量
                weight += goodsNum.get(goodsVo.getGoodsTypeId()) * goodsVo.getWeight();
                // 数量*单价
                goodsAmount += goodsNum.get(goodsVo.getGoodsTypeId()) * goodsVo.getPrice();
            } else { // 下架
                goodsPieces -= goodsNum.get(goodsVo.getGoodsTypeId());
            }
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
        freightVo.setBalance(Double.valueOf(remain) / 100000);
        return freightVo;
    }


    /**
     * 根据商品类型ID是查询商品信息
     *
     * @param goodsTypeIds
     * @return
     */
    public Map<Integer, ConfirmGoodsVo> findGoodsByTypeIds(List<Integer> goodsTypeIds) {
        Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap = Maps.newHashMap();
        List<ConfirmGoodsVo> confirmGoodsVos = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        if (CollectionUtils.isNotEmpty(confirmGoodsVos)) {
            for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
                confirmGoodsVoMap.put(confirmGoodsVo.getGoodsTypeId(), confirmGoodsVo);
            }
        }
        return confirmGoodsVoMap;
    }

    /**
     * 根据不同的express提示信息
     *
     * @param express
     * @return
     */
    private String findTipsByExpress(String express) {
        String tips = "";
        switch (express) {
            case express_shunfeng:
                tips = "我们将在2个工作日之内发货。";
                break;
            case express_shentong:
                tips = "我们将在2个工作日之内发货，预计5天内到货。";
                break;
            case express_dbwl:
                tips = "我们将在2个工作日之内发货，预计7天内到货";
                break;
        }
        return tips;
    }

}
