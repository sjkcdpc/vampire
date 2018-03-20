package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.vampire.bean.GoodsFreightSubtotalBo;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.ExpressUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.basicdata.DistrictApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.independenceDay.vo.OrderSuccessVo;
import com.gaosi.api.revolver.constant.ExpressConstant;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.dto.QueryExpressPriceDto;
import com.gaosi.api.revolver.facade.ExpressServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.ExpressPrice;
import com.gaosi.api.revolver.model.ExpressType;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.turing.constant.InstitutionTypeEnum;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.bean.common.Assert;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.gaosi.api.vulcan.constant.GoodsConstant;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.*;
import com.gaosi.api.vulcan.model.Consignee;
import com.gaosi.api.vulcan.model.GoodsPeriod;
import com.gaosi.api.vulcan.model.GoodsPic;
import com.gaosi.api.vulcan.model.ShoppingCartList;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;

/**
 * 订单
 * Created by gaoxinzhong on 2017/5/15.
 */
@Service("orderManager")
public class OrderManager {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ConsigneeServiceFacade consigneeServiceFacade;

    @Resource
    private ShoppingCartServiceFacade shoppingCartServiceFacade;

    @Resource
    private FinancialAccountService finAccService;

    @Resource
    private DistrictApi districtApi;

    @Resource
    private GoodsServiceFacade goodsServiceFacade;

    @Resource
    private OrderServiceFacade orderServiceFacade;

    @Resource
    private InstitutionService institutionService;

    @Resource
    private ExpressUtil expressUtil;

    @Resource
    private BaseMapper baseMapper;

    @Resource
    private GoodsPicServiceFacade goodsPicServiceFacade;

    @Resource
    private GoodsPeriodServiceFacade goodsPeriodServiceFacade;

    @Resource
    private ExpressServiceFacade expressServiceFacade;

    @Resource
    private FinancialAccountManager financialAccountManager;


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
        for (ConsigneeVo consigneeVo : consigneeVos) {
            if (consigneeVo.getSystemDefault()) {
                confirmOrderVo.setDefCneeId(consigneeVo.getId());
                break;
            }
        }
        confirmOrderVo.setConsignees(consigneeVos);
        // 2. 配送方式
        ApiResponse<List<ExpressType>> expressTypeResponse = expressServiceFacade.queryAllExpressType();
        ApiResponseCheck.check(expressTypeResponse);
        List<ExpressType> expressTypes = expressTypeResponse.getBody();
        List<ConfirmExpressVo> confirmExpressVos = baseMapper.mapAsList(expressTypes,ConfirmExpressVo.class);
        confirmOrderVo.setExpressTypes(confirmExpressVos);
        // 3. 用户购物车中商品清单
        List<ShoppingCartListVo> shoppingCartListVos = getShoppingCartDetails(userId, null, null);
        Map<Integer, Integer> goodsNum =CollectionCommonUtil.toMapByList(shoppingCartListVos,"getGoodsTypeId",Integer.class,
                "getNum",Integer.class);

        // 4. 根据goodsTypeIds查询商品其他信息
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsNum);
        ApiResponseCheck.check(apiResponse);

        List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
        GoodsFreightSubtotalBo goodsFreightSubtotalBo = getGoodsFreightSubtotalBo(goodsVos, goodsNum);

        confirmOrderVo.setGoodsItem(goodsVos);
        confirmOrderVo.setGoodsPieces(goodsFreightSubtotalBo.getGoodsPieces());
        confirmOrderVo.setGoodsAmount(goodsFreightSubtotalBo.getGoodsAmount());
        confirmOrderVo.setGoodsWeight(goodsFreightSubtotalBo.getWeight());
        // 5. 账户余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(insId);
        Long remain = rr.getUsableRemain();
        confirmOrderVo.setBalance(Double.valueOf(remain) / 10000);
        // 6. 获取token
        confirmOrderVo.setToken(finAccService.getTokenForFinancial());
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
     * @param token        财务token
     * @return
     */
    public OrderSuccessVo submit(Integer userId, Integer insId, Integer consigneeId, String receivePhone,
                                 String express, List<Integer> goodsTypeIds, String token) {
        logger.info("submitOrder --> userId : {}, insId : {}, consigneeId : {}, receivePhone : {}, express : {}, goodsTypeIds : {}",
                userId, insId, consigneeId, receivePhone, express, goodsTypeIds);
        // TODO 目前只查教材的类别
        int categoryId = MallItemConstant.Category.JCZB.getId();
        List<ShoppingCartListVo> shoppingCartListVos = getShoppingCartDetails(userId,categoryId,goodsTypeIds);
        // 创建订单对象
        GoodsOrderVo goodsOrderVo = createGoodsOrder(shoppingCartListVos, userId, insId, consigneeId, receivePhone, express);
        logger.info("submitOrder --> goodsOrder info : {}", JSONObject.toJSONString(goodsOrderVo));
        // 支付金额 = 商品金额 + 邮费
        Double amount = (goodsOrderVo.getConsumeAmount() + goodsOrderVo.getFreight()) * 10000;
        // 账号余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(insId);
        financialAccountManager.checkRemainMoney(rr,amount.longValue());
        // 是否同步到WMS
        Boolean syncToWms = true;
        Institution insinfo = institutionService.getInsInfoById(insId);
        if (insinfo == null) {
            logger.error("未知机构信息{}", insId);
        } else if (InstitutionTypeEnum.TEST.getType() == insinfo.getInstitutionType() ||
                InstitutionTypeEnum.TRY.getType() == insinfo.getInstitutionType() ||
                !expressUtil.getSyncToWms()) {
            // 测试机构,试用机构或者关闭同步开关 则不同步到WMS
            syncToWms = false;
        }

        logger.info("submitOrder --> syncToWms : {}", syncToWms);
        // 创建订单
        ApiResponse<SimpleGoodsOrderVo> apiResponse = orderServiceFacade.createOrder(goodsOrderVo, token, syncToWms);
        ApiResponseCheck.check(apiResponse);
        SimpleGoodsOrderVo simpleGoodsOrderVo = apiResponse.getBody();
        logger.info("submitOrder --> orderId : {}", simpleGoodsOrderVo);
        List<ShoppingCartList> shoppingCartLists = Lists.newArrayList();
        for (ShoppingCartListVo shoppingCartListVo : shoppingCartListVos) {
            ShoppingCartList shoppingCartList = new ShoppingCartList();
            // TODO 现在默认教材，将来扩展需要存其他类型的时候此处需要改，类别需要前端传过来。
            shoppingCartList.setCategoryId(MallItemConstant.Category.JCZB.getId());
            shoppingCartList.setGoodsTypeId(shoppingCartListVo.getGoodsTypeId());
            shoppingCartLists.add(shoppingCartList);
        }
        shoppingCartServiceFacade.clearShoppingCart(shoppingCartLists, userId);
        //如果包含DIY，则返回空提示
        return new OrderSuccessVo(simpleGoodsOrderVo.getOrderId(), simpleGoodsOrderVo.isContainDIY() ? "" : goodsOrderVo.getAging(), getSplitTips(simpleGoodsOrderVo.getSplitNum()));
    }

    /**
     * 创建订单对象
     *
     * @param shoppingCartListVos 购物车中的商品
     * @param userId            用户ID
     * @param insId             机构ID
     * @param consigneeId       收货人ID
     * @param receivePhone      收货通知手机号
     * @param express           快递
     * @return
     */
    private GoodsOrderVo createGoodsOrder(List<ShoppingCartListVo> shoppingCartListVos, Integer userId, Integer insId,
                                          Integer consigneeId, String receivePhone, String express) {
        // 订单
        GoodsOrderVo goodsOrderVo = new GoodsOrderVo();
        // 订单基本信息
        goodsOrderVo.setCategoryId(MallItemConstant.Category.JCZB.getId());
        goodsOrderVo.setRemark(StringUtils.EMPTY);
        goodsOrderVo.setUserId(userId);
        // 商品件数
        int goodsPieces = 0;
        // 商品重量
        double weight = 0;
        // 商品总金额
        double goodsAmount = 0;
        // 商品SKUID和数量的映射，Map<goodTypeId,num>
        Map<Integer, Integer> goodsNum =CollectionCommonUtil.toMapByList(shoppingCartListVos,"getGoodsTypeId",Integer.class,
                "getNum",Integer.class);
        // 查询商品明细
        ApiResponse<List<ConfirmGoodsVo>> goodsVosResponse = goodsServiceFacade.queryGoodsInfo(goodsNum);
        ApiResponseCheck.check(goodsVosResponse);
        List<ConfirmGoodsVo> confirmGoodsVos = goodsVosResponse.getBody();
        validateGoods(confirmGoodsVos);
        // 订单详情
        List<OrderDetailVo> orderDetails = Lists.newArrayList();
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            Integer goodsTypeId = confirmGoodsVo.getGoodsTypeId();
            int num = goodsNum.get(goodsTypeId);
            confirmGoodsVo.setNum(num);
            // 数量*单重量
            weight += num * confirmGoodsVo.getWeight();
            // 数量*单价
            goodsAmount += num * confirmGoodsVo.getPrice();
            //件数
            goodsPieces += num;
            // 商品明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setBarCode(confirmGoodsVo.getBarCode());
            orderDetailVo.setGoodsId(confirmGoodsVo.getGoodsId());
            orderDetailVo.setGoodTypeId(goodsTypeId);
            orderDetailVo.setName(confirmGoodsVo.getGoodsName() + Constants.ORDERDETAIL_NAME_DIV + confirmGoodsVo.getGoodsTypeName());
            orderDetailVo.setNum(num);
            orderDetailVo.setPrice(confirmGoodsVo.getPrice());
            orderDetailVo.setWeight(confirmGoodsVo.getWeight());
            orderDetailVo.setCustomized(confirmGoodsVo.getCustomized());
            orderDetails.add(orderDetailVo);
        }
        handlePeriod(orderDetails);
        goodsOrderVo.setOrderDetailVos(orderDetails);
        // 商品总金额
        goodsOrderVo.setConsumeAmount(goodsAmount);
        // 收货人信息
        Consignee consignee = consigneeServiceFacade.selectById(consigneeId);
        Assert.notNull(consignee,"请选择收货地址");
        goodsOrderVo.setAreaId(consignee.getAreaId());
        goodsOrderVo.setConsigneeName(consignee.getName());
        goodsOrderVo.setConsigneePhone(consignee.getPhone());
        ApiResponse<AddressDTO> addressDTOApiResponse = districtApi.getAncestryById(consignee.getAreaId());
        ApiResponseCheck.check(addressDTOApiResponse);
        AddressDTO address = addressDTOApiResponse.getBody();
        Assert.notNull(address,"收货人地址查询失败，请联系管理员");
        goodsOrderVo.setAddress(address);
        goodsOrderVo.setConsigneeAddress(getConsigneeAddress(address,consignee));
        goodsOrderVo.setReceivePhone(StringUtils.isBlank(receivePhone) ? consignee.getPhone() : receivePhone); // 发货通知手机号
        // 机构信息
        Institution institution = institutionService.getInsInfoById(insId);
        goodsOrderVo.setInstitutionId(insId);
        goodsOrderVo.setInstitutionName(institution.getName());
        // 设置配送方式
        goodsOrderVo.setExpressCode(express);
        goodsOrderVo.setExpressType(ExpressConstant.Express.getIdByCode(express).toString());
        // 计算运费
        Integer provinceId = address.getProvinceId();
        Integer areaId = address.getDistrictId();
        Map<String, Integer> expressMap = new HashMap<>();
        if (express.equals(OrderConstant.LogisticsMode.EXPRESS_DBWL)) {
            expressMap.put(express, areaId);
        } else {
            expressMap.put(express, provinceId);
        }
        ApiResponse<List<ExpressFreightVo>> apiResponse = expressServiceFacade.calFreight(expressMap, weight, goodsPieces);
        ApiResponseCheck.check(apiResponse);
        ExpressFreightVo expressFreightVo = apiResponse.getBody().get(0);
        goodsOrderVo.setFreight(expressFreightVo.getTotalFreight());
        // 订单提交成功时，快递时效提示
        goodsOrderVo.setAging(MessageFormat.format(expressUtil.getExpressTips(), expressFreightVo.getAging()));
        return goodsOrderVo;
    }

    /**
     * 获取收货地址
     *
     * @param address   地址
     * @param consignee 收货人
     * @return 地址
     */
    private String getConsigneeAddress(AddressDTO address, Consignee consignee) {
        StringBuilder preAddress = new StringBuilder();
        preAddress.append(address.getProvince() == null ? StringUtils.EMPTY : address.getProvince());
        preAddress.append(address.getCity() == null ? StringUtils.EMPTY : address.getCity());
        preAddress.append(address.getDistrict() == null ? StringUtils.EMPTY : address.getDistrict());
        String result = null;
        if (StringUtils.isBlank(preAddress.toString())) {
            result = consignee.getAddress();
        } else {
            result = preAddress.append(" ").append(consignee.getAddress()).toString();
        }
        return result;
    }

    /**
     * 处理学期统计
     *
     * @param orderDetailVos
     */
    private void handlePeriod(List<OrderDetailVo> orderDetailVos) {
        List<Integer> goodsIds = new ArrayList<>(CollectionCommonUtil.getFieldSetByObjectList(orderDetailVos, "getGoodsId", Integer.class));
        List<GoodsPeriod> goodsPeriodList = goodsPeriodServiceFacade.findByGoodsId(goodsIds).getBody();
        Map<Integer, List<GoodsPeriod>> goodsPeriodMap = CollectionCommonUtil.groupByList(goodsPeriodList, "getGoodsId", Integer.class);

        DateTime now = new DateTime();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MM月dd日");
        for (OrderDetailVo orderDetailVo : orderDetailVos) {
            List<GoodsPeriod> goodsPeriods = goodsPeriodMap.get(orderDetailVo.getGoodsId());
            Integer allPeriod = 0;
            if (CollectionUtils.isNotEmpty(goodsPeriods)) {
                try {
                    for (GoodsPeriod goodsPeriod : goodsPeriods) {
                        if (StringUtils.isNotBlank(goodsPeriod.getStartTime())) {
                            DateTime startTime = DateTime.parse(goodsPeriod.getStartTime(), dateTimeFormatter);
                            DateTime endTime = DateTime.parse(goodsPeriod.getEndTime(), dateTimeFormatter);
                            //将年份设置为当年（忽略年份对判断的影响）
                            startTime = startTime.withYear(now.getYear());
                            endTime = endTime.withYear(now.getYear()).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
                            if (startTime.isAfter(endTime)) {//开始日期大于结束日期,表示跨年
                                if (startTime.isBeforeNow() || endTime.isAfterNow()) {//如果满足其实任一条件
                                    Integer period = goodsPeriod.getPeriod();
                                    allPeriod = allPeriod | Constants.PERIOD_MAP.get(period);
                                }
                            } else {
                                if (startTime.isBeforeNow() && endTime.isAfterNow()) {//如果在该学期设定的时间段中
                                    Integer period = goodsPeriod.getPeriod();
                                    allPeriod = allPeriod | Constants.PERIOD_MAP.get(period);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("创建订单时，计算统计属性出错", e);
                }
            }
            String periodStatistics = String.format("%04d", Integer.valueOf(Integer.toBinaryString(allPeriod)));
            orderDetailVo.setPeriodStatistics(periodStatistics);
        }
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
            Set<Integer> areaIds = CollectionCommonUtil.getFieldSetByObjectList(consigneeVos, "getAreaId", Integer.class);
            Map<Integer, AddressDTO> addressDTOMap = findAddressByIds(areaIds);
            for (ConsigneeVo consigneeVo : consigneeVos) {
                AddressDTO addressDTO = addressDTOMap.get(consigneeVo.getAreaId());
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
     * @param areaIds 区ID
     * @return
     */
    private Map<Integer,AddressDTO> findAddressByIds(Collection<Integer> areaIds) {
        ApiResponse<Map<Integer, AddressDTO>> apiResponse = districtApi.getAncestryMapByIds(areaIds);
        if (apiResponse != null && apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
            return apiResponse.getBody();
        }
        throw new BusinessException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
    }

    /**
     * 计算运费
     *
     * @param provinceId     省ID
     * @param weight         商品重量
     * @param expressVoLists 物流信息
     */
    private void calcFreight(Integer provinceId,Integer areaId, double weight, List<ConfirmExpressVo> expressVoLists, int goodsPieces) {
        logger.info("calcFreight --> provinceId : {}, weight : {}, expressLists : {}", provinceId, weight, expressVoLists);
        Map<String,Integer> expressMap = new HashMap<>();
        for (ConfirmExpressVo confirmExpressVo : expressVoLists) {
            expressMap.put(confirmExpressVo.getCode(),provinceId);
            if(confirmExpressVo.getCode().equals(OrderConstant.LogisticsMode.EXPRESS_DBWL)){
                expressMap.put(confirmExpressVo.getCode(),areaId);
            }
        }
        ApiResponse<List<ExpressFreightVo>> apiResponse = expressServiceFacade.calFreight(expressMap, weight, goodsPieces);
        ApiResponseCheck.check(apiResponse);
        List<ExpressFreightVo> expressFreightVos = apiResponse.getBody();
        Map<String,ExpressFreightVo> expressFreightVoMap = CollectionCommonUtil.toMapByList(expressFreightVos,"getExpressCode",String.class);
        for (ConfirmExpressVo confirmExpressVo:expressVoLists) {
            ExpressFreightVo expressFreightVo = expressFreightVoMap.get(confirmExpressVo.getCode());
            confirmExpressVo.setFirstFreight(expressFreightVo.getFirstFreight());
            confirmExpressVo.setBeyondPrice(expressFreightVo.getBeyondPrice());
            confirmExpressVo.setTotalBeyondWeight(expressFreightVo.getTotalBeyondWeight());
            confirmExpressVo.setTotalFreight(expressFreightVo.getTotalFreight());
            confirmExpressVo.setRemark(expressFreightVo.getRemark());
            String aging = MessageFormat.format(expressUtil.getAging(),expressFreightVo.getAging());
            confirmExpressVo.setAging(aging);
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
    public FreightVo reloadFreight(Integer userId, Integer insId, Integer provinceId,Integer areaId, List<Integer> goodsTypeIds) {
        FreightVo freightVo = new FreightVo();
        int goodsPieces = 0; // 商品总件数
        double weight = 0; // 重量
        double goodsAmount = 0; // 总金额
        if (CollectionUtils.isNotEmpty(goodsTypeIds)) {
            // TODO 目前只查教材的类别
            int categoryId = MallItemConstant.Category.JCZB.getId();
            List<ShoppingCartListVo> shoppingCartListVos = getShoppingCartDetails(userId, categoryId, goodsTypeIds);
            Map<Integer, Integer> goodsNum =CollectionCommonUtil.toMapByList(shoppingCartListVos,"getGoodsTypeId",Integer.class,
                    "getNum",Integer.class);
            ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsNum);
            ApiResponseCheck.check(apiResponse);
            List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();

            GoodsFreightSubtotalBo goodsFreightSubtotalBo = getGoodsFreightSubtotalBo(goodsVos, goodsNum);
            weight = goodsFreightSubtotalBo.getWeight();
            goodsAmount = goodsFreightSubtotalBo.getGoodsAmount();
            goodsPieces = goodsFreightSubtotalBo.getGoodsPieces();
        }
        ApiResponse<List<ExpressType>> expressTypeResponse = expressServiceFacade.queryAllExpressType();
        ApiResponseCheck.check(expressTypeResponse);
        List<ExpressType> expressTypes = expressTypeResponse.getBody();
        List<ConfirmExpressVo> confirmExpressVos = baseMapper.mapAsList(expressTypes,ConfirmExpressVo.class);
        // 计算邮费
        calcFreight(provinceId,areaId, weight, confirmExpressVos, goodsPieces);
        // set
        freightVo.setGoodsPieces(goodsPieces);
        freightVo.setGoodsWeight(weight);
        freightVo.setGoodsAmount(goodsAmount);
        freightVo.setExpressTypes(confirmExpressVos);
        // 账号余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(insId);
        Long remain = rr.getUsableRemain();
        freightVo.setBalance(Double.valueOf(remain) / 10000);
        return freightVo;
    }

    /**
     * 获取计算运费需要的总重量、总费用、总件数
     *
     * @param goodsVos 商品Vos
     * @param goodsNum 购物车详情对应的map
     * @return 运费小计bo
     */
    private GoodsFreightSubtotalBo getGoodsFreightSubtotalBo(List<ConfirmGoodsVo> goodsVos, Map<Integer, Integer> goodsNum) {
        GoodsFreightSubtotalBo goodsFreightSubtotalBo = new GoodsFreightSubtotalBo();
        int goodsPieces = 0; // 商品总件数
        double weight = 0; // 重量
        double goodsAmount = 0; // 总金额
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            Integer goodsTypeId = goodsVo.getGoodsTypeId();
            Integer typeNum = goodsNum.get(goodsTypeId);
            goodsVo.setNum(typeNum);
            // 数量*单价
            goodsVo.setTotal(typeNum * goodsVo.getPrice());
            if (goodsVo.getStatus() == GoodsConstant.Status.ON) { // 上架
                // 数量*单重量
                weight += typeNum * goodsVo.getWeight();
                goodsAmount += goodsVo.getTotal();
                goodsPieces += typeNum;
            }
        }

        goodsFreightSubtotalBo.setGoodsAmount(goodsAmount);
        goodsFreightSubtotalBo.setWeight(weight);
        goodsFreightSubtotalBo.setGoodsPieces(goodsPieces);
        return goodsFreightSubtotalBo;
    }

    /**
     * 获取购物车详情列表
     *
     * @param userId       用户Id
     * @param categoryId   类型ID
     * @param goodsTypeIds 商品skuID列表
     * @return 购物车详情列表
     */
    private List<ShoppingCartListVo> getShoppingCartDetails(Integer userId, Integer categoryId, List<Integer> goodsTypeIds) {
        ApiResponse<List<ShoppingCartListVo>> listApiResponse = null;

        if (null != categoryId && CollectionUtils.isNotEmpty(goodsTypeIds)) {
            listApiResponse = shoppingCartServiceFacade.queryShoppingCartDetail(userId, categoryId, goodsTypeIds);
        } else {//用于确认页面，此时没有goodsTypeIds
            listApiResponse = shoppingCartServiceFacade.queryShoppingCartDetail(userId);
        }
        ApiResponseCheck.check(listApiResponse);
        List<ShoppingCartListVo> shoppingCartListVos = listApiResponse.getBody();
        Assert.notEmpty(shoppingCartListVos, "购物车中商品已结算或为空");

        return shoppingCartListVos;
    }

    /**
     * 根据商品id查询图片
     * @param goodsIds
     * @return
     */
    public Map<Integer, List<GoodsPic>> findPicByGoodsIds(List<Integer> goodsIds) {
        ApiResponse<List<GoodsPic>> apiResponse = goodsPicServiceFacade.findGoodsPicByGoodsIds(goodsIds);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        List<GoodsPic> goodsPics = apiResponse.getBody();
        Map<Integer, List<GoodsPic>> picMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(goodsPics)){
            picMap = CollectionCommonUtil.groupByList(goodsPics, "getGoodsId", Integer.class);
        }
        return picMap;
    }

    /**
     * 根据拆单数量提示信息
     *
     * @param splitNum
     * @return
     */
    private String getSplitTips(Integer splitNum) {
        String remark = "";
        if (splitNum > 1) {
            remark = MessageFormat.format(expressUtil.getSplitTips(), splitNum);
        }
        return remark;
    }

    /**
     * 校验商品是否存在、条形码是否为空、是否已下架、重量是否有误
     *
     * @param confirmGoodsVos
     */
    private void validateGoods(List<ConfirmGoodsVo> confirmGoodsVos) {
        Assert.notEmpty(confirmGoodsVos, "商品不存在");
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            // 商品全称（商品+型号名称）
            String goodsFullName = confirmGoodsVo.getGoodsName() + "-" + confirmGoodsVo.getGoodsTypeName();
            Assert.isTrue(StringUtils.isNotBlank(confirmGoodsVo.getBarCode()), goodsFullName + "条形码为空");
            Assert.isTrue(confirmGoodsVo.getStatus() != GoodsConstant.Status.OFF, "存在已下架商品");
            Assert.isTrue(confirmGoodsVo.getWeight() > 0, goodsFullName + "重量有误");
        }
    }

    /**
     * 处理多个订单使用
     * @param goodsOrderVos
     * @param needDetail 是否需要图等详细信息
     */
    public void dealGoodsOrderVos(List<GoodsOrderVo> goodsOrderVos, boolean needDetail){
        if (CollectionUtils.isNotEmpty(goodsOrderVos)) {
            Set<Integer> goodsIds = Sets.newHashSet();
            // 查询快递时效的条件
            List<QueryExpressPriceDto> queryExpressPriceDtoList = new ArrayList<>();
            // 订单中物流方式下的目的地ID
            Set<Integer> areaIds = Sets.newHashSet();
            // 准备工作,防止多次调用微服务
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                QueryExpressPriceDto queryExpressPriceDto = new QueryExpressPriceDto();
                queryExpressPriceDto.setExpressId(Integer.parseInt(goodsOrderVo.getExpressType()));
                queryExpressPriceDto.setDestAreaId(goodsOrderVo.getAreaId());
                queryExpressPriceDtoList.add(queryExpressPriceDto);
                // 获取订单中非物流的目的地ID
                if(!goodsOrderVo.getExpressType().equals(ExpressConstant.Express.WULIU.getId().toString())){
                    areaIds.add(goodsOrderVo.getAreaId());
                }
                if (goodsOrderVo.getSplitNum() > 1){
                    // 拆单的处理子订单
                    List<SubGoodsOrderVo> subGoodsOrderVos = goodsOrderVo.getSubGoodsOrderVos();
                    for (SubGoodsOrderVo subGoodsOrderVo : subGoodsOrderVos) {
                        goodsIds.addAll(CollectionCommonUtil.getFieldSetByObjectList(subGoodsOrderVo.getSubOrderDetailVos(),"getGoodsId",Integer.class));
                    }
                }else {
                    goodsIds.addAll(CollectionCommonUtil.getFieldSetByObjectList(goodsOrderVo.getOrderDetailVos(),"getGoodsId",Integer.class));
                }
            }
            Map<Integer, List<GoodsPic>> picMap = null;
            if (needDetail) {
                // 订单详情查看需要获取图片
                picMap = this.findPicByGoodsIds(new ArrayList<>(goodsIds));
            }
            // 查询区ID对应的省ID
            Map<Integer, AddressDTO> provinceIdMap = new HashMap<>();
            if(CollectionUtils.isNotEmpty(areaIds)) {
                provinceIdMap = findAddressByIds(new ArrayList<>(areaIds));
                // 把顺丰和普快递的目的地ID重置为省ID
                for (QueryExpressPriceDto queryExpressPriceDto : queryExpressPriceDtoList) {
                    if (provinceIdMap.containsKey(queryExpressPriceDto.getDestAreaId())&&
                            !queryExpressPriceDto.getExpressId().equals(ExpressConstant.Express.WULIU.getId())) {
                        queryExpressPriceDto.setDestAreaId(provinceIdMap.get(queryExpressPriceDto.getDestAreaId()).getProvinceId());
                    }
                }
            }
            // 获取快递时效
            ApiResponse<List<ExpressPrice>> apiResponse = expressServiceFacade.queryExpressPriceList(queryExpressPriceDtoList);
            Map<String,ExpressPrice> expressPriceMap = new HashMap<>();
            if(apiResponse.getRetCode()==ApiRetCode.SUCCESS_CODE&&CollectionUtils.isNotEmpty(apiResponse.getBody())){
                List<ExpressPrice> expressPriceList = apiResponse.getBody();
                expressPriceMap = CollectionCommonUtil.toUnionKeyMapByList(expressPriceList,Lists.newArrayList("getExpressId","getDestAreaId"));
            }
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                this.dealGoodsOrderVo(goodsOrderVo, picMap,expressPriceMap,provinceIdMap);
            }
        }
    }

    /**
     * 补充订单中的小计，图片，时效
     * @param goodsOrderVo
     * @param picMap
     * @param expressPriceMap
     * @param provinceIdMap
     */
    private void dealGoodsOrderVo(GoodsOrderVo goodsOrderVo, Map<Integer, List<GoodsPic>> picMap,
                                  Map<String,ExpressPrice> expressPriceMap, Map<Integer,AddressDTO> provinceIdMap){
        String expressType = goodsOrderVo.getExpressType();
        Integer districtId ;
        // 非物流方式的订单，需要查询目的地的省ID
        if(!expressType.equals(ExpressConstant.Express.WULIU.getId().toString())&&
                provinceIdMap.containsKey(goodsOrderVo.getAreaId())){
            districtId = provinceIdMap.get(goodsOrderVo.getAreaId()).getProvinceId();
        }else{
            districtId = goodsOrderVo.getAreaId();
        }

        // 时效
        String aging = StringUtils.EMPTY;
        // 获取时效时效信息
        String unionKey = goodsOrderVo.getExpressType()+districtId;
        if(expressPriceMap.containsKey(unionKey)){
            aging = MessageFormat.format(expressUtil.getExpressTips(),expressPriceMap.get(unionKey).getAging());
        }
        //拆单需要处理子订单
        if (goodsOrderVo.getSplitNum() > 1){
            // 拆单的去除父订单多余数据
            goodsOrderVo.getOrderDetailVos().clear();
            for (SubGoodsOrderVo subGoodsOrderVo : goodsOrderVo.getSubGoodsOrderVos()) {
                // 将子订单的时效重置为父订单更新后的时效
                //DIY定制不写仓库提示
                if (subGoodsOrderVo.getOrderType() == OrderConstant.OrderType.DIY_CUSTOM_ORDER){
                    subGoodsOrderVo.setWarehouseTips(StringUtils.EMPTY);
                }else {
                    subGoodsOrderVo.setWarehouseTips(subGoodsOrderVo.getWarehouseTips() + "," + aging);
                }
                dealSubGoodsOrderVo(subGoodsOrderVo, picMap);
            }
        }else {
            //DIY定制不写仓库提示
            if (goodsOrderVo.getOrderType() == OrderConstant.OrderType.DIY_CUSTOM_ORDER){
                goodsOrderVo.setWarehouseTips(StringUtils.EMPTY);
            }else {
                goodsOrderVo.setWarehouseTips(goodsOrderVo.getWarehouseTips() + "," + aging);
            }
            //详情中的一些信息
            for (OrderDetailVo orderDetailVo : goodsOrderVo.getOrderDetailVos()) {
                orderDetailVo.setTotal(AmountUtil.multiply(orderDetailVo.getPrice(), orderDetailVo.getNum().doubleValue(), 2));
                if (null != picMap){
                    List<GoodsPic> subPicList = picMap.get(orderDetailVo.getGoodsId());
                    if (CollectionUtils.isNotEmpty(subPicList)) {
                        Collections.sort(subPicList);
                        orderDetailVo.setPicUrl(subPicList.get(0).getPicUrl());
                    }
                }
            }
        }
    }

    /**
     * 处理子订单
     * @param subGoodsOrderVo
     * @param picMap
     */
    private void dealSubGoodsOrderVo(SubGoodsOrderVo subGoodsOrderVo, Map<Integer, List<GoodsPic>> picMap){
        List<SubOrderDetailVo> subOrderDetailVos = subGoodsOrderVo.getSubOrderDetailVos();
        Double consumeAmount = 0d ;
        //子订单详情中的一些信息
        for (SubOrderDetailVo subOrderDetailVo : subOrderDetailVos) {
            subOrderDetailVo.setTotal(AmountUtil.multiply(subOrderDetailVo.getPrice(), subOrderDetailVo.getNum().doubleValue(), 2));
            consumeAmount = AmountUtil.add(consumeAmount,subOrderDetailVo.getTotal(), 2);
            if (null != picMap){
                List<GoodsPic> subPicList = picMap.get(subOrderDetailVo.getGoodsId());
                if (CollectionUtils.isNotEmpty(subPicList)) {
                    Collections.sort(subPicList);
                    subOrderDetailVo.setPicUrl(subPicList.get(0).getPicUrl());
                }
            }
        }
        subGoodsOrderVo.setConsumeAmount(consumeAmount);
    }
}
