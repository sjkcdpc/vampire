package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.ExpressUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.independenceDay.model.Institution;
import com.gaosi.api.independenceDay.service.InstitutionService;
import com.gaosi.api.independenceDay.vo.OrderSuccessVo;
import com.gaosi.api.vulcan.facade.ConsigneeServiceFacade;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.vulcan.facade.ShoppingCartServiceFacade;
import com.gaosi.api.vulcan.model.Consignee;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.model.OrderDetail;
import com.gaosi.api.vulcan.model.ShoppingCartList;
import com.gaosi.api.vulcan.vo.ConfirmExpressVo;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import com.gaosi.api.vulcan.vo.ConsigneeVo;
import com.gaosi.api.vulcan.vo.FreightVo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 订单
 * Created by gaoxinzhong on 2017/5/15.
 */
@Service("orderManager")
public class OrderManager {

    @Autowired
    private ConsigneeServiceFacade consigneeServiceFacade;

    @Autowired
    private ShoppingCartServiceFacade shoppingCartServiceFacade;

    @Autowired
    private FinancialAccountService finAccService;

    @Autowired
    private AreaApi areaApi;

    @Autowired
    private GoodsServiceFacade goodsServiceFacade;

    @Autowired
    private OrderServiceFacade orderServiceFacade;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private ExpressUtil expressUtil;

    @Resource
    private BaseMapper baseMapper;

    @Resource(name = "dictionaryManager")
    private DictionaryManager dictionaryManager;

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
        List<ShoppingCartList> shoppingCartLists = shoppingCartServiceFacade.queryShoppingCartDetail(userId);
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
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
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
        //calcFreight(provinceId, weight, confirmOrderVo.getExpress(),goodsPieces);
        // 5. 账户余额
        RemainResult rr = finAccService.getRemainByInsId(insId);
        if(rr==null)
        {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        Long remain = rr.getUsableRemain();
        confirmOrderVo.setBalance(Double.valueOf(remain) / 10000);
        // 6. 获取token
        confirmOrderVo.setToken(finAccService.getTokenForFinancial());
        logger.info("confirmOrder end --> confirmOrderVo : {}", confirmOrderVo);
        // 清空之前计算的邮费
        defaultExpressForConfirmOrder(confirmOrderVo.getExpress());
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
     * @param token        财务token
     * @return
     */
    public OrderSuccessVo submit(Integer userId, Integer insId, Integer consigneeId, String receivePhone,
                                 String express, List<Integer> goodsTypeIds, String token) {
        logger.info("submitOrder --> userId : {}, insId : {}, consigneeId : {}, receivePhone : {}, express : {}, goodsTypeIds : {}",
                userId, insId, consigneeId, receivePhone, express, goodsTypeIds);
        // 账号余额
        RemainResult rr = finAccService.getRemainByInsId(insId);
        if(rr==null) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        if (CollectionUtils.isEmpty(goodsTypeIds)) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "所选商品不能为空");
        }
        List<ShoppingCartList> shoppingCartLists = shoppingCartServiceFacade.queryShoppingCartDetail(userId, goodsTypeIds);
        if (CollectionUtils.isEmpty(shoppingCartLists)) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
        }

        // 创建订单对象
        GoodsOrder goodsOrder = createGoodsOrder(shoppingCartLists, userId, insId, consigneeId, receivePhone, express);
        logger.info("submitOrder --> goodsOrder info : {}", JSONObject.toJSONString(goodsOrder));
        // 支付金额 = 商品金额 + 邮费
        Double amount = (goodsOrder.getConsumeAmount() + goodsOrder.getFreight()) * 10000;
        Long remain = rr.getUsableRemain();
        if (amount.longValue() > remain) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "余额不足");
        }
        // 是否走发网
        Boolean syncToWms = true;
        // ruanyj 测试环境是否发网的开关
        Institution insinfo = institutionService.getInsInfoById(insId);
        // 1测试机构 2试用机构 或者关闭发网开关 则不走发网
        if ((insinfo!=null&&Constants.INS_TYPES.contains(insinfo.getInstitutionType().intValue())) || !expressUtil.getSyncToWms()) {
            syncToWms = false;
        }
        logger.info("submitOrder --> syncToWms : {}", syncToWms);
        // 创建订单
        ApiResponse<String> apiResponse = orderServiceFacade.createOrder(goodsOrder, token, syncToWms);
        if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
            logger.info("submitOrder --> orderId : {}", apiResponse.getBody());
            try {
                List<Integer> shoppingCartListIds = Lists.newArrayList();
                for (ShoppingCartList shoppingCartList : shoppingCartLists) {
                    shoppingCartListIds.add(shoppingCartList.getId());
                }
                shoppingCartServiceFacade.clearShoppingCart(shoppingCartListIds);
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
        // 收货人信息判断
        Consignee consignee = consigneeServiceFacade.selectById(consigneeId);
        if(consignee==null) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "请选择收货地址");
        }
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
        ApiResponse<List<ConfirmGoodsVo>> listApiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        if(listApiResponse.getRetCode()!=ApiRetCode.SUCCESS_CODE) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "商品不存在! ");
        }
        List<ConfirmGoodsVo> goodsVos = listApiResponse.getBody();
        // 再次校验商品是否已下架，库存。
        validateGoods(goodsVos, goodsNum);
        for (ConfirmGoodsVo confirmGoodsVo : goodsVos) {
            int num = goodsNum.get(confirmGoodsVo.getGoodsTypeId());
            confirmGoodsVo.setNum(num);
            // 数量*单重量
            weight += num * confirmGoodsVo.getWeight();
            // 数量*单价
            goodsAmount += num * confirmGoodsVo.getPrice();
            // 商品明细
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setBarCode(confirmGoodsVo.getBarCode());
            orderDetail.setGoodsId(confirmGoodsVo.getGoodsId());
            orderDetail.setGoodTypeId(confirmGoodsVo.getGoodsTypeId());
            orderDetail.setName(confirmGoodsVo.getGoodsName()+Constants.ORDERDETAIL_NAME_DIV+confirmGoodsVo.getGoodsTypeName());
            orderDetail.setNum(num);
            orderDetail.setPrice(confirmGoodsVo.getPrice());
            orderDetails.add(orderDetail);
        }
        goodsOrder.setOrderDetails(orderDetails);
        goodsOrder.setAreaId(consignee.getAreaId());
        ApiResponse<AddressDTO> ad = areaApi.getProvinceCityById(consignee.getAreaId());
        goodsOrder.setConsigneeName(consignee.getName());
        goodsOrder.setConsigneePhone(consignee.getPhone());
        //ruanyj 收货人地址补全
        AddressDTO add = ad.getBody();
        StringBuilder preAddress = new StringBuilder();
        if(add!=null) {
            preAddress.append(add.getProvince() == null ? "" : add.getProvince());
            preAddress.append(add.getCity() == null ? "" : add.getCity());
            preAddress.append(add.getDistrict() == null ? "" :add.getDistrict());
        }
        if(StringUtils.isBlank(preAddress.toString()))
        {
            goodsOrder.setConsigneeAddress(consignee.getAddress());
        }
        else {
            goodsOrder.setConsigneeAddress(preAddress.toString() + " " + consignee.getAddress());
        }
        goodsOrder.setConsumeAmount(goodsAmount); // 商品总金额
        goodsOrder.setInstitutionId(insId);
        goodsOrder.setRemark(StringUtils.EMPTY);
        goodsOrder.setReceivePhone(StringUtils.isBlank(receivePhone) ? consignee.getPhone() : receivePhone); // 发货通知手机号
        goodsOrder.setUserId(userId);
        boolean isFree = false; // 是否免物流费
        if (express.equals(Constants.EXPRESS_DBWL)) {
            if (goodsPieces >= 100) {
                goodsOrder.setExpressCode(Constants.EXPRESS_DBWL);
                isFree = true;
            } else if (goodsPieces >= 50) {
                goodsOrder.setExpressCode(Constants.EXPRESS_SHENTONG);
                isFree = true;
            }
            else {
                // ruanyj 其他情况选择申通，不免运费
                goodsOrder.setExpressCode(Constants.EXPRESS_SHENTONG);
            }
        } else {
            goodsOrder.setExpressCode(express);
        }
        // 是否计算邮费
        if (isFree) {
            // 选择德邦物流，goodsPieces >= 50， 邮费0。
            goodsOrder.setFreight(0D);
        } else {
            List<AddressDTO> addressDTOS = findAddressByIds(consignee.getAreaId());
            if(!CollectionUtils.isEmpty(addressDTOS)) {
                // 省ID
                Integer provinceId = addressDTOS.get(0).getProvinceId();
                // 计算邮费
                ApiResponse<List<HashMap<String, Object>>> apiResponse = orderServiceFacade.calculateFreight(provinceId, weight,
                        express.equals(Constants.EXPRESS_DBWL) ? Constants.EXPRESS_SHENTONG : express);
                logger.info("submitOrder --> freight : {}", apiResponse);
                HashMap<String, Object> freightMap = apiResponse.getBody().get(0);
                goodsOrder.setFreight(Double.valueOf(freightMap.get("totalFreight").toString()));
            }
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
            List<ConsigneeVo> consigneeVos = baseMapper.mapAsList(consignees, ConsigneeVo.class);
            // 区ids
            Integer[] areaIds = new Integer[consigneeVos.size()];
            for (int i = 0; i < consigneeVos.size(); i++) {
                areaIds[i] = consigneeVos.get(i).getAreaId();
            }
            List<AddressDTO> addressDTOS = findAddressByIds(areaIds);
            if (!CollectionUtils.isEmpty(addressDTOS)) {
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
        //确认订单时，查不到收货人地址不抛异常
        if(apiResponse!=null) {
            if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
                if (CollectionUtils.isEmpty(apiResponse.getBody())) {
                    throw new IllegalArgException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
                }
                return apiResponse.getBody();
            } else {
                throw new IllegalArgException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
            }
        }
        else{
            return null;
        }
    }

    /**
     * 计算运费
     *
     * @param provinceId     省ID
     * @param weight         商品重量
     * @param expressVoLists 物流信息
     */
    private void calcFreight(Integer provinceId, double weight, List<ConfirmExpressVo> expressVoLists, int goodsPieces) {
        logger.info("calcFreight --> provinceId : {}, weight : {}, expressLists : {}", provinceId, weight, expressVoLists);
        ApiResponse<List<HashMap<String, Object>>> apiResponse = orderServiceFacade.calculateFreight(provinceId, weight, Constants.EXPRESS);
        logger.info("calcFreight --> freight : {}", apiResponse);
        List<HashMap<String, Object>> listMap = apiResponse.getBody();
        for (int i = 0; i < expressVoLists.size(); i++) {
            ConfirmExpressVo confirmExpressVo = expressVoLists.get(i);
            HashMap<String, Object> map = null;
            if (i == 2) {
                map = listMap.get(1);
                confirmExpressVo.setFirstFreight(map.get("firstFreight").toString());
                confirmExpressVo.setBeyondPrice(map.get("beyondPrice").toString());
                confirmExpressVo.setBeyondWeight(map.get("beyondWeight").toString());
                //ruanyj 德邦物流超过50本免运费
                if (goodsPieces >= 50) {
                    confirmExpressVo.setRemark(Constants.FREE_FREIGHT);
                    confirmExpressVo.setTotalFreight(Constants.DEFAULT_DOUBLE_VALUE);
                } else {
                    confirmExpressVo.setRemark(StringUtils.EMPTY);
                    confirmExpressVo.setTotalFreight(map.get("totalFreight").toString());
                }
            } else {
                map = listMap.get(i);
                confirmExpressVo.setFirstFreight(map.get("firstFreight").toString());
                confirmExpressVo.setBeyondPrice(map.get("beyondPrice").toString());
                confirmExpressVo.setBeyondWeight(map.get("beyondWeight").toString());
                confirmExpressVo.setTotalFreight(map.get("totalFreight").toString());
                confirmExpressVo.setRemark(StringUtils.EMPTY);
            }
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
        int goodsPieces = 0; // 商品总件数
        double weight = 0; // 重量
        double goodsAmount = 0; // 总金额
        if (CollectionUtils.isNotEmpty(goodsTypeIds)) {
            shoppingCartLists = shoppingCartServiceFacade.queryShoppingCartDetail(userId, goodsTypeIds);
            if (CollectionUtils.isEmpty(shoppingCartLists)) {
                throw new IllegalArgException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
            }
            goodsTypeIds = Lists.newArrayList();

            // 数量 goodsTypeIds - > num
            Map<Integer, Integer> goodsNum = Maps.newHashMap();
            for (ShoppingCartList shoppingCartList : shoppingCartLists) {
                goodsTypeIds.add(shoppingCartList.getGoodsTypeId());
                goodsNum.put(shoppingCartList.getGoodsTypeId(), shoppingCartList.getNum());
                goodsPieces += shoppingCartList.getNum();
            }

            ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
            if(apiResponse.getRetCode()!=ApiRetCode.SUCCESS_CODE) {
                throw new IllegalArgException(ExceptionCode.UNKNOWN, "商品不存在! ");
            }
            List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
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
        }
        // 计算邮费
        calcFreight(provinceId, weight, confirmExpressVos,goodsPieces);
        // set
        freightVo.setGoodsPieces(goodsPieces);
        freightVo.setGoodsWeight(weight);
        freightVo.setGoodsAmount(goodsAmount);
        freightVo.setExpress(confirmExpressVos);
        // 账号余额
        RemainResult rr = finAccService.getRemainByInsId(insId);
        if(rr==null) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        Long remain = rr.getUsableRemain();
        freightVo.setBalance(Double.valueOf(remain) / 10000);
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
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
        if (CollectionUtils.isNotEmpty(goodsVos)) {
            for (ConfirmGoodsVo confirmGoodsVo : goodsVos) {
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
            case Constants.EXPRESS_SHUNFENG:
                tips = "我们将在2个工作日之内发货。";
                break;
            case Constants.EXPRESS_SHENTONG:
                tips = "我们将在2个工作日之内发货，预计5天内到货。";
                break;
            case Constants.EXPRESS_DBWL:
                tips = "我们将在2个工作日之内发货，预计7天内到货";
                break;
            default:
                throw new IllegalArgException(ExceptionCode.UNKNOWN, "请选择发货服务方式");
        }
        return tips;
    }

    /**
     * 校验库存和是否下架
     *
     * @param confirmGoodsVos
     * @param goodsNum
     */
    private void validateGoods(List<ConfirmGoodsVo> confirmGoodsVos, Map<Integer, Integer> goodsNum) {
        if (CollectionUtils.isNotEmpty(confirmGoodsVos)) {
            List<ConfirmGoodsVo> offGoods = Lists.newArrayList();
            List<String> barCodes = Lists.newArrayList();
            // 1. 校验商品是否已经下架
            for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
                //ruanyj 商品编码为空校验
                if(StringUtils.isBlank(confirmGoodsVo.getBarCode())) {
                    throw new IllegalArgException(ExceptionCode.UNKNOWN, confirmGoodsVo.getGoodsName()+confirmGoodsVo.getGoodsTypeName()+"的SKU编码为空");
                }
                barCodes.add(confirmGoodsVo.getBarCode());
                if (confirmGoodsVo.getStatus() == 0) {
                    offGoods.add(confirmGoodsVo);
                }
            }
            if (CollectionUtils.isNotEmpty(offGoods)) {
                String jsonString = JSONObject.toJSONString(confirmGoodsVos);
                throw new IllegalArgumentException(jsonString);
            }
            if (expressUtil.getIsInventory()) {
                // 2. 校验库存 {barCode, inventory}
                boolean flag = false;
                ApiResponse<Map<String, Integer>> apiResponse = orderServiceFacade.queryInventory(barCodes);
                // ruanyj 查询库存校验
                if(apiResponse.getRetCode()!=ApiRetCode.SUCCESS_CODE){
                    throw new IllegalArgException(ExceptionCode.UNKNOWN, "查询库存失败");
                }
                Map<String, Integer> inventoryMap = apiResponse.getBody();
                for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
                    // 库存量
                    int inventory = inventoryMap.get(confirmGoodsVo.getBarCode());
                    // 购买数量
                    int num = goodsNum.get(confirmGoodsVo.getGoodsTypeId());
                    if (num > inventory) {
                        confirmGoodsVo.setStatus(2);
                        confirmGoodsVo.setInventory(inventory);
                        flag = true;
                    } // 库存不足
                }
                if (flag) {
                    String jsonString = JSONObject.toJSONString(confirmGoodsVos);
                    throw new IllegalArgumentException(jsonString);
                }
            }
        }
    }

    /**
     * 处理完成，每次需要清空
     *
     * @param expressVos
     */
    private void defaultExpressForConfirmOrder(List<ConfirmExpressVo> expressVos) {
        for (ConfirmExpressVo expressVo : expressVos) {
            expressVo.setBeyondPrice(StringUtils.EMPTY);
            expressVo.setBeyondWeight(StringUtils.EMPTY);
            expressVo.setFirstFreight(StringUtils.EMPTY);
            expressVo.setTotalFreight(StringUtils.EMPTY);
            expressVo.setRemark(StringUtils.EMPTY);
        }
    }

    /**
     * 查询运费，提供给cat使用。
     *
     * @param provinceId 省ID
     * @param weight     重量
     * @param delivery   物流
     * @return
     */
    public List<HashMap<String, Object>> calcFreightForCat(Integer provinceId, Double weight, String delivery) {
        ApiResponse<List<HashMap<String, Object>>> apiResponse = orderServiceFacade.calculateFreight(provinceId, weight, delivery);
        return apiResponse.getBody();
    }
}
