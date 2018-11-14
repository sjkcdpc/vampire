package com.aixuexi.vampire.manager;

import com.aixuexi.vampire.bean.GoodsFreightSubtotalBo;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.ExpressUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.independenceDay.vo.OrderSuccessVo;
import com.gaosi.api.revolver.constant.ExpressConstant;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.dto.OrderDetailVoDto;
import com.gaosi.api.revolver.dto.QueryExpressPriceDto;
import com.gaosi.api.revolver.facade.ExpressServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.ExpressPrice;
import com.gaosi.api.revolver.model.ExpressType;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.turing.constant.InstitutionTypeEnum;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.vulcan.bean.common.Assert;
import com.gaosi.api.vulcan.constant.GoodsConstant;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.dto.QueryMallSkuVoDto;
import com.gaosi.api.vulcan.facade.*;
import com.gaosi.api.vulcan.model.*;
import com.gaosi.api.vulcan.vo.*;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.gaosi.api.revolver.constant.OrderConstant.FINANCE_EXCHANGE_RATE;
import static com.gaosi.api.revolver.constant.OrderConstant.OrderType.DIY_CUSTOM_ORDER;
import static com.gaosi.api.revolver.constant.OrderConstant.OrderType.PRESALE_ORDER;
import static com.gaosi.api.vulcan.constant.MallItemConstant.Category.JCSD;

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
    private FinancialAccountService financialAccountService;

    @Resource
    private BasicDataManager basicDataManager;

    @Resource
    private GoodsServiceFacade goodsServiceFacade;

    @Resource
    private OrderServiceFacade orderServiceFacade;

    @Resource
    private ExpressUtil expressUtil;

    @Resource
    private BaseMapper baseMapper;

    @Resource
    private GoodsPeriodServiceFacade goodsPeriodServiceFacade;

    @Resource
    private ExpressServiceFacade expressServiceFacade;

    @Resource
    private FinancialAccountManager financialAccountManager;

    @Resource
    private MallItemPicServiceFacade mallItemPicServiceFacade;

    @Resource
    private MallSkuExtTalentServiceFacade mallSkuExtTalentServiceFacade;

    @Resource
    private MallSkuPicServiceFacade mallSkuPicServiceFacade;

    @Resource
    private MallItemExtServiceFacade mallItemExtServiceFacade;

    @Resource
    private MallSkuServiceFacade mallSkuServiceFacade;


    /**
     * 核对订单信息
     *
     * @param userId 用户ID
     * @param insId  机构ID
     * @return
     */
    public ConfirmOrderVo confirmOrder(Integer userId, Integer insId) {
        ConfirmOrderVo confirmOrderVo = new ConfirmOrderVo();
        // 收货人地址
        List<ConsigneeVo> consigneeVos = findConsignee(insId);
        for (ConsigneeVo consigneeVo : consigneeVos) {
            if (consigneeVo.getSystemDefault()) {
                confirmOrderVo.setDefCneeId(consigneeVo.getId());
                break;
            }
        }
        confirmOrderVo.setConsignees(consigneeVos);
        // 配送方式
        ApiResponse<List<ExpressType>> expressTypeResponse = expressServiceFacade.queryAllExpressType();
        List<ExpressType> expressTypes = expressTypeResponse.getBody();
        List<ConfirmExpressVo> confirmExpressVos = baseMapper.mapAsList(expressTypes,ConfirmExpressVo.class);
        confirmOrderVo.setExpressTypes(confirmExpressVos);
        // 根据userId查询订单确认中的商品信息
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryConfirmGoodsVo(userId,null);
        List<ConfirmGoodsVo> confirmGoodsVos = apiResponse.getBody();
        Assert.notEmpty(confirmGoodsVos,"购物车中商品已结算或为空");
        dealConfirmGoodsVos(confirmGoodsVos,confirmOrderVo);
        GoodsFreightSubtotalBo goodsFreightSubtotalBo = getGoodsFreightSubtotalBo(confirmGoodsVos);
        confirmOrderVo.setGoodsPieces(goodsFreightSubtotalBo.getGoodsPieces());
        confirmOrderVo.setGoodsAmount(goodsFreightSubtotalBo.getGoodsAmount());
        confirmOrderVo.setGoodsWeight(goodsFreightSubtotalBo.getWeight());
        // 账户余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(insId);
        confirmOrderVo.setAidouUsableRemain(Double.valueOf(rr.getAidouUsableRemain()) / 10000);
        confirmOrderVo.setRmbUsableRemain(Double.valueOf(rr.getRmbUsableRemain()) / 10000);
        // 获取token
        confirmOrderVo.setToken(financialAccountService.getTokenForFinancial());
        return confirmOrderVo;
    }

    /**
     * 筛选出普通商品列表和预售商品列表
     * @param confirmGoodsVos
     * @param confirmOrderVo
     */
    private void dealConfirmGoodsVos(List<ConfirmGoodsVo> confirmGoodsVos, ConfirmOrderVo confirmOrderVo) {
        List<ConfirmGoodsVo> goodsItem = new ArrayList<>();
        List<ConfirmGoodsVo> preSaleGoodsItem = new ArrayList<>();
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            if(confirmGoodsVo.getPreSale()){
                preSaleGoodsItem.add(confirmGoodsVo);
            }else{
                goodsItem.add(confirmGoodsVo);
            }
        }
        confirmOrderVo.setGoodsItem(goodsItem);
        confirmOrderVo.setPreSaleGoodsItem(preSaleGoodsItem);
    }

    /**
     * 提交订单
     * @param submitGoodsOrderVo
     * @return
     */
    public OrderSuccessVo submit(SubmitGoodsOrderVo submitGoodsOrderVo) {
        // 创建订单对象
        GoodsOrderVo goodsOrderVo = createGoodsOrder(submitGoodsOrderVo);
        // 支付金额 = 商品金额 + 邮费
        Double amount = (goodsOrderVo.getConsumeAmount() + goodsOrderVo.getFreight()) * 10000;
        // 账号余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(submitGoodsOrderVo.getInsId());
        financialAccountManager.checkRemainMoney(rr, amount.longValue());
        // 创建订单
        ApiResponse<GoodsOrderVo> apiResponse = orderServiceFacade.createOrder(goodsOrderVo, submitGoodsOrderVo.getToken());
        GoodsOrderVo createOrderResult = apiResponse.getBody();
        // 清空购物车
        List<ShoppingCartList> shoppingCartLists = Lists.newArrayList();
        for (Integer goodsTypeId : submitGoodsOrderVo.getGoodsTypeIds()) {
            ShoppingCartList shoppingCartList = new ShoppingCartList();
            shoppingCartList.setCategoryId(MallItemConstant.Category.JCZB.getId());
            shoppingCartList.setGoodsTypeId(goodsTypeId);
            shoppingCartLists.add(shoppingCartList);
        }
        shoppingCartServiceFacade.clearShoppingCart(shoppingCartLists, submitGoodsOrderVo.getUserId());
        return getTips(createOrderResult);
    }

    /**
     * 创建订单对象
     * @param submitGoodsOrderVo
     * @return
     */
    private GoodsOrderVo createGoodsOrder(SubmitGoodsOrderVo submitGoodsOrderVo) {
        // 订单
        GoodsOrderVo goodsOrderVo = new GoodsOrderVo();
        // 订单基本信息
        goodsOrderVo.setCategoryId(MallItemConstant.Category.JCZB.getId());
        goodsOrderVo.setRemark(StringUtils.EMPTY);
        Integer userId = submitGoodsOrderVo.getUserId();
        goodsOrderVo.setUserId(userId);
        // 查询商品明细
        List<Integer> goodsTypeIds = submitGoodsOrderVo.getGoodsTypeIds();
        ApiResponse<List<ConfirmGoodsVo>> goodsVosResponse = goodsServiceFacade.queryConfirmGoodsVo(userId, goodsTypeIds);
        List<ConfirmGoodsVo> confirmGoodsVos = goodsVosResponse.getBody();
        validateGoods(confirmGoodsVos);
        GoodsFreightSubtotalBo goodsFreightSubtotalBo = getGoodsFreightSubtotalBo(confirmGoodsVos);
        // 订单详情
        List<OrderDetailVo> orderDetailVos = baseMapper.mapAsList(confirmGoodsVos,OrderDetailVo.class);
        handlePeriod(orderDetailVos);
        goodsOrderVo.setOrderDetailVos(orderDetailVos);
        // 商品总金额
        goodsOrderVo.setConsumeAmount(goodsFreightSubtotalBo.getGoodsAmount());
        // 机构信息
        goodsOrderVo.setInstitutionId(submitGoodsOrderVo.getInsId());
        Institution institution = submitGoodsOrderVo.getInstitution();
        goodsOrderVo.setInstitutionName(institution.getName());
        goodsOrderVo.setSyncToWms(true);
        if (InstitutionTypeEnum.TEST.getType() == institution.getInstitutionType() || !expressUtil.getSyncToWms()) {
            // 测试机构或者关闭同步开关 则不同步到WMS
            goodsOrderVo.setSyncToWms(false);
        }
        // 设置配送方式
        String express = submitGoodsOrderVo.getExpress();
        goodsOrderVo.setExpressCode(express);
        goodsOrderVo.setExpressType(ExpressConstant.Express.getIdByCode(express).toString());
        // 收货人信息
        Consignee consignee = consigneeServiceFacade.selectById(submitGoodsOrderVo.getConsigneeId());
        Assert.notNull(consignee,"请选择收货地址");
        Integer areaId = consignee.getAreaId();
        goodsOrderVo.setAreaId(areaId);
        goodsOrderVo.setConsigneeName(consignee.getName());
        goodsOrderVo.setConsigneePhone(consignee.getPhone());
        String receivePhone = submitGoodsOrderVo.getReceivePhone();
        goodsOrderVo.setReceivePhone(StringUtils.isBlank(receivePhone) ? consignee.getPhone() : receivePhone);
        // 地址信息
        Map<Integer, AddressDTO> addressDTOMap = basicDataManager.getAddressByAreaIds(Lists.newArrayList(areaId));
        AddressDTO address = addressDTOMap.get(areaId);
        goodsOrderVo.setAddress(address);
        goodsOrderVo.setConsigneeAddress(getConsigneeAddress(address,consignee));
        // 计算运费
        Integer provinceId = address.getProvinceId();
        Map<String, Integer> expressMap = new HashMap<>();
        if (express.equals(OrderConstant.LogisticsMode.EXPRESS_DBWL)) {
            expressMap.put(express, areaId);
        } else {
            expressMap.put(express, provinceId);
        }
        ApiResponse<List<ExpressFreightVo>> apiResponse = expressServiceFacade.calFreight(expressMap,
                goodsFreightSubtotalBo.getWeight(), goodsFreightSubtotalBo.getGoodsPieces());
        ExpressFreightVo expressFreightVo = apiResponse.getBody().get(0);
        goodsOrderVo.setFreight(expressFreightVo.getTotalFreight());
        // 订单提交成功时，快递时效提示
        goodsOrderVo.setAging(expressFreightVo.getAging());
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
        List<Integer> goodsIds = orderDetailVos.stream().map(OrderDetailVo::getGoodsId).distinct().collect(Collectors.toList());
        List<GoodsPeriod> goodsPeriodList = goodsPeriodServiceFacade.findByGoodsId(goodsIds).getBody();
        Map<Integer, List<GoodsPeriod>> goodsPeriodMap = goodsPeriodList.stream().collect(Collectors.groupingBy(GoodsPeriod::getGoodsId));

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
            Set<Integer> areaIds = consigneeVos.stream().map(ConsigneeVo::getAreaId).collect(Collectors.toSet());
            Map<Integer, AddressDTO> addressDTOMap = basicDataManager.getAddressByAreaIds(Lists.newArrayList(areaIds));
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
     * 计算运费
     * @param freightVo
     * @param provinceId
     * @param areaId
     */
    private void calcFreight(FreightVo freightVo, Integer provinceId, Integer areaId) {
        ApiResponse<List<ExpressType>> expressTypeResponse = expressServiceFacade.queryAllExpressType();
        List<ExpressType> expressTypes = expressTypeResponse.getBody();
        Map<String, Integer> expressMap = new HashMap<>();
        for (ExpressType expressType : expressTypes) {
            String code = expressType.getCode();
            expressMap.put(code, provinceId);
            if (OrderConstant.LogisticsMode.EXPRESS_DBWL.equals(code)) {
                expressMap.put(code, areaId);
            }
        }
        ApiResponse<List<ExpressFreightVo>> apiResponse = expressServiceFacade.calFreight(expressMap, freightVo.getGoodsWeight(), freightVo.getGoodsPieces());
        List<ExpressFreightVo> expressFreightVos = apiResponse.getBody();
        Map<String,ExpressFreightVo> expressFreightVoMap = expressFreightVos.stream().collect(Collectors.toMap(ExpressFreightVo::getExpressCode, e -> e, (k1, k2) -> k1));
        List<ConfirmExpressVo> confirmExpressVos = baseMapper.mapAsList(expressTypes, ConfirmExpressVo.class);
        for (ConfirmExpressVo confirmExpressVo : confirmExpressVos) {
            ExpressFreightVo expressFreightVo = expressFreightVoMap.get(confirmExpressVo.getCode());
            confirmExpressVo.setFirstFreight(expressFreightVo.getFirstFreight());
            confirmExpressVo.setBeyondPrice(expressFreightVo.getBeyondPrice());
            confirmExpressVo.setTotalBeyondWeight(expressFreightVo.getTotalBeyondWeight());
            confirmExpressVo.setTotalFreight(expressFreightVo.getTotalFreight());
            confirmExpressVo.setRemark(expressFreightVo.getRemark());
            String aging = MessageFormat.format(expressUtil.getAging(), expressFreightVo.getAging());
            confirmExpressVo.setAging(aging);
        }
        freightVo.setExpressTypes(confirmExpressVos);
    }

    /**
     * 修改商品数量，选择部分商品，重新选择收货人，重新计算运费。
     *
     * @param reqFreightVo
     * @return
     */
    public FreightVo reloadFreight(ReqFreightVo reqFreightVo) {
        FreightVo freightVo = new FreightVo();
        int goodsPieces = 0; // 商品总件数
        double weight = 0; // 重量
        double goodsAmount = 0; // 总金额
        List<Integer> goodsTypeIds = reqFreightVo.getGoodsTypeIds();
        if (CollectionUtils.isNotEmpty(goodsTypeIds)) {
            ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryConfirmGoodsVo(reqFreightVo.getUserId(), goodsTypeIds);
            List<ConfirmGoodsVo> confirmGoodsVos = apiResponse.getBody();
            if (CollectionUtils.isNotEmpty(confirmGoodsVos)) {
                GoodsFreightSubtotalBo goodsFreightSubtotalBo = getGoodsFreightSubtotalBo(confirmGoodsVos);
                weight = goodsFreightSubtotalBo.getWeight();
                goodsAmount = goodsFreightSubtotalBo.getGoodsAmount();
                goodsPieces = goodsFreightSubtotalBo.getGoodsPieces();
            }
        }
        freightVo.setGoodsPieces(goodsPieces);
        freightVo.setGoodsWeight(weight);
        freightVo.setGoodsAmount(goodsAmount);
        // 计算邮费
        calcFreight(freightVo,reqFreightVo.getProvinceId(),reqFreightVo.getAreaId());
        // 账号余额
        Integer insitutionId = reqFreightVo.getInsitutionId();
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(insitutionId);
        freightVo.setAidouUsableRemain(AmountUtil.divide(Double.valueOf(rr.getAidouUsableRemain()),FINANCE_EXCHANGE_RATE));
        freightVo.setRmbUsableRemain(AmountUtil.divide(Double.valueOf(rr.getRmbUsableRemain()),FINANCE_EXCHANGE_RATE));
        return freightVo;
    }

    /**
     * 获取计算运费需要的总重量、总费用、总件数
     *
     * @param confirmGoodsVos 商品Vos
     * @return 运费小计bo
     */
    private GoodsFreightSubtotalBo getGoodsFreightSubtotalBo(List<ConfirmGoodsVo> confirmGoodsVos) {
        GoodsFreightSubtotalBo goodsFreightSubtotalBo = new GoodsFreightSubtotalBo();
        int goodsPieces = 0; // 商品总件数
        double weight = 0; // 重量
        double goodsAmount = 0; // 总金额
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            Integer num = confirmGoodsVo.getNum();
            // 数量*单价
            confirmGoodsVo.setTotal(num * confirmGoodsVo.getPrice());
            if (confirmGoodsVo.getStatus() == GoodsConstant.Status.ON) { // 上架
                // 数量*单重量
                weight += num * confirmGoodsVo.getWeight();
                goodsAmount += confirmGoodsVo.getTotal();
                goodsPieces += num;
            }
        }
        goodsFreightSubtotalBo.setGoodsAmount(goodsAmount);
        goodsFreightSubtotalBo.setWeight(weight);
        goodsFreightSubtotalBo.setGoodsPieces(goodsPieces);
        return goodsFreightSubtotalBo;
    }

    /**
     * 下单成功后提示
     * @param goodsOrderVo
     * @return
     */
    private OrderSuccessVo getTips(GoodsOrderVo goodsOrderVo) {
        OrderSuccessVo orderSuccessVo = baseMapper.map(goodsOrderVo,OrderSuccessVo.class);
        Integer splitNum = goodsOrderVo.getSplitNum();
        if (splitNum != null && splitNum > 1) {
            orderSuccessVo.setSplitTips(MessageFormat.format(expressUtil.getSplitTips(), splitNum));
        }
        Integer orderType = goodsOrderVo.getOrderType();
        String aging = goodsOrderVo.getAging();
        String agingTips = getAgingTips(orderType, aging);
        orderSuccessVo.setTips(agingTips);
        return orderSuccessVo;
    }

    /**
     * 获取时效提示
     * @param orderType
     * @param aging
     * @return
     */
    private String getAgingTips(Integer orderType,String aging) {
        String agingTips = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(aging)) {
            agingTips = MessageFormat.format(expressUtil.getAging(), aging);
        }
        switch (orderType) {
            case DIY_CUSTOM_ORDER:
                return expressUtil.getDiyTips();
            case PRESALE_ORDER:
                return expressUtil.getPreSaleDeliveryTime() + agingTips;
            default:
                return expressUtil.getDeliveryTime() + agingTips;
        }
    }

    /**
     * 校验商品是否存在、条形码是否为空、是否已下架、重量是否有误
     *
     * @param confirmGoodsVos
     */
    private void validateGoods(List<ConfirmGoodsVo> confirmGoodsVos) {
        Assert.notEmpty(confirmGoodsVos, "商品不存在");
        // 是否包含预售商品
        boolean containsPreSale = false;
        // 是否包含普通商品
        boolean containsCustom = false;
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            // 商品全称（商品+型号名称）
            String goodsFullName = confirmGoodsVo.getGoodsName() + "-" + confirmGoodsVo.getGoodsTypeName();
            Assert.isTrue(StringUtils.isNotBlank(confirmGoodsVo.getBarCode()), goodsFullName + "条形码为空");
            Assert.isTrue(confirmGoodsVo.getStatus() != GoodsConstant.Status.OFF, "存在已下架商品");
            Assert.isTrue(confirmGoodsVo.getWeight() >= 0, goodsFullName + "重量有误");
            if (confirmGoodsVo.getPreSale()) {
                containsPreSale = true;
            }else {
                containsCustom = true;
            }
        }
        // 同时包含预售和普通
        boolean containsBoth = containsPreSale && containsCustom;
        Assert.isTrue(!containsBoth, "预售产品不能和普通产品同时下单结算，请分开下单结算");
    }

    /**
     * 处理多个订单使用
     * @param goodsOrderVos
     */
    public void dealGoodsOrderVos(List<GoodsOrderVo> goodsOrderVos) {
        if (CollectionUtils.isNotEmpty(goodsOrderVos)) {
            // 查询商品信息
            Map<Integer, MallSkuVo> mallSkuVoMap = queryMallSkuVoMap(goodsOrderVos);
            // 查询快递时效
            Map<String, ExpressPrice> expressPriceMap = queryExpressPriceMap(goodsOrderVos);
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                dealGoodsOrderVo(goodsOrderVo, expressPriceMap, mallSkuVoMap);
            }
        }
    }

    /**
     * 查询快递时效
     *
     * @param goodsOrderVos
     */
    private Map<String, ExpressPrice> queryExpressPriceMap(List<GoodsOrderVo> goodsOrderVos) {
        // 区ID集合
        List<Integer> areaIds = goodsOrderVos.stream().map(GoodsOrderVo::getAreaId).collect(Collectors.toList());
        Map<Integer, AddressDTO> addressDTOMap = basicDataManager.getAddressByAreaIds(areaIds);
        // 查询快递时效的条件
        List<QueryExpressPriceDto> queryExpressPriceDtoList = new ArrayList<>();
        for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
            AddressDTO addressDTO = addressDTOMap.get(goodsOrderVo.getAreaId());
            goodsOrderVo.setAddress(addressDTO);
            int expressId = Integer.parseInt(goodsOrderVo.getExpressType());
            QueryExpressPriceDto queryExpressPriceDto = new QueryExpressPriceDto();
            queryExpressPriceDto.setExpressId(expressId);
            if (expressId == ExpressConstant.Express.WULIU.getId()) {
                // 把物流的目的地ID设为区ID
                queryExpressPriceDto.setDestAreaId(addressDTO.getDistrictId());
            } else {
                // 把顺丰和普通快递的目的地ID设为省ID
                queryExpressPriceDto.setDestAreaId(addressDTO.getProvinceId());
            }
            queryExpressPriceDtoList.add(queryExpressPriceDto);
        }
        // 获取快递时效
        ApiResponse<List<ExpressPrice>> apiResponse = expressServiceFacade.queryExpressPriceList(queryExpressPriceDtoList);
        List<ExpressPrice> expressPriceList = apiResponse.getBody();
        return expressPriceList.stream().collect(Collectors.toMap(ExpressPrice::getExpressIdAndDestAreaId, e -> e, (k1, k2) -> k1));
    }



    /**
     * 获取商品信息
     *
     * @param goodsOrderVos
     */
    private Map<Integer, MallSkuVo> queryMallSkuVoMap(List<GoodsOrderVo> goodsOrderVos) {
        List<Integer> mallSkuIds = new ArrayList<>();
        for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
            List<OrderDetailVo> orderDetailVos = goodsOrderVo.getOrderDetailVos();
            mallSkuIds.addAll(orderDetailVos.stream().map(OrderDetailVo::getMallSkuId).collect(Collectors.toSet()));
        }
        QueryMallSkuVoDto queryMallSkuVoDto = new QueryMallSkuVoDto();
        queryMallSkuVoDto.setIds(mallSkuIds);
        queryMallSkuVoDto.setNeedPic(true);
        ApiResponse<List<MallSkuVo>> apiResponse = mallSkuServiceFacade.queryMallSkuVoBySkuIds(queryMallSkuVoDto);
        List<MallSkuVo> mallSkuVos = apiResponse.getBody();
        return mallSkuVos.stream().collect(Collectors.toMap(MallSkuVo::getId, m -> m, (k1, k2) -> k1));
    }

    /**
     * 补充订单中的小计，图片，时效
     * @param goodsOrderVo
     * @param expressPriceMap
     * @param mallSkuVoMap
     */
    private void dealGoodsOrderVo(GoodsOrderVo goodsOrderVo, Map<String,ExpressPrice> expressPriceMap, Map<Integer, MallSkuVo> mallSkuVoMap){
        // 时效
        String aging = StringUtils.EMPTY;
        String expressType = goodsOrderVo.getExpressType();
        int expressId = Integer.parseInt(expressType);
        AddressDTO address = goodsOrderVo.getAddress();
        // 获取时效时效信息
        String unionKey;
        if (expressId == ExpressConstant.Express.WULIU.getId()) {
            // 把物流的目的地ID设为区ID
            unionKey = expressType + address.getDistrictId();
        } else {
            // 把顺丰和普通快递的目的地ID设为省ID
            unionKey = expressType + address.getProvinceId();
        }
        if (expressPriceMap.containsKey(unionKey)) {
            aging = expressPriceMap.get(unionKey).getAging();
        }
        //拆单需要处理子订单
        if (goodsOrderVo.getSplitNum() > 1){
            // 拆单的去除父订单多余数据
            goodsOrderVo.getOrderDetailVos().clear();
            for (SubGoodsOrderVo subGoodsOrderVo : goodsOrderVo.getSubGoodsOrderVos()) {
                // 将子订单的时效重置为父订单更新后的时效
                Integer orderType = subGoodsOrderVo.getOrderType();
                String agingTips = getAgingTips(orderType, aging);
                if (orderType != OrderConstant.OrderType.DIY_CUSTOM_ORDER){
                    subGoodsOrderVo.setWarehouseTips(subGoodsOrderVo.getWarehouseTips() + agingTips);
                }
                List<SubOrderDetailVo> subOrderDetailVos = subGoodsOrderVo.getSubOrderDetailVos();
                List<OrderDetailVoDto> orderDetailVoDtos = baseMapper.mapAsList(subOrderDetailVos, OrderDetailVoDto.class);
                dealOrderDetailVoDto(orderDetailVoDtos,mallSkuVoMap);
                subGoodsOrderVo.setSubOrderDetailVos(baseMapper.mapAsList(orderDetailVoDtos,SubOrderDetailVo.class));
            }
        }else {
            Integer orderType = goodsOrderVo.getOrderType();
            String agingTips = getAgingTips(orderType, aging);
            if (orderType != OrderConstant.OrderType.DIY_CUSTOM_ORDER){
                goodsOrderVo.setWarehouseTips(goodsOrderVo.getWarehouseTips() + agingTips);
            }
            List<OrderDetailVo> orderDetailVos = goodsOrderVo.getOrderDetailVos();
            List<OrderDetailVoDto> orderDetailVoDtos = baseMapper.mapAsList(orderDetailVos, OrderDetailVoDto.class);
            dealOrderDetailVoDto(orderDetailVoDtos,mallSkuVoMap);
            goodsOrderVo.setOrderDetailVos(baseMapper.mapAsList(orderDetailVoDtos,OrderDetailVo.class));
        }
    }

    /**
     * 补充订单详情中的商品信息
     * @param orderDetailVoDtos
     * @param mallSkuVoMap
     */
    private void dealOrderDetailVoDto(List<OrderDetailVoDto> orderDetailVoDtos, Map<Integer, MallSkuVo> mallSkuVoMap) {
        for (OrderDetailVoDto orderDetailVoDto : orderDetailVoDtos) {
            MallSkuVo mallSkuVo = mallSkuVoMap.get(orderDetailVoDto.getMallSkuId());
            Assert.notNull(mallSkuVo,"未知商品信息" + orderDetailVoDto.getMallSkuId());
            orderDetailVoDto.setMallSkuName(mallSkuVo.getName());
            Integer categoryId = mallSkuVo.getCategoryId();
            if(categoryId.equals(JCSD.getId())){
                List<MallSkuPic> mallSkuPics = mallSkuVo.getMallSkuPics();
                orderDetailVoDto.setPicUrl(mallSkuPics.get(0).getPicUrl());
            }else{
                List<MallItemPic> mallItemPics = mallSkuVo.getMallItemPics();
                orderDetailVoDto.setPicUrl(mallItemPics.get(0).getPicUrl());
            }
        }
    }

    /**
     * 补充分类名称，图片，拆单状态,人才中心SKU信息
     * @param itemOrderVos
     * @param isDetail
     */
    public void dealItemOrderVos(List<ItemOrderVo> itemOrderVos,Boolean isDetail){
        // 提取商品ID，人才中心的SKUID
        List<ItemOrderDetailVo> itemOrderDetailsTotal = new ArrayList<>();
        List<ItemOrderDetailVo> itemOrderDetails4RCZX = new ArrayList<>();
        // 是否存在校长培训订单
        Boolean existNailOrder = false;
        for (ItemOrderVo itemOrderVo : itemOrderVos) {
            Integer categoryId = itemOrderVo.getCategoryId();
            if(categoryId.equals(MallItemConstant.Category.LDPXSC.getId())){
                existNailOrder = true;
            }
            if(categoryId.equals(MallItemConstant.Category.RCZX.getId())){
                itemOrderDetails4RCZX.addAll(itemOrderVo.getItemOrderDetails());
            }else{
                itemOrderDetailsTotal.addAll(itemOrderVo.getItemOrderDetails());
            }
        }
        List<Integer> mallItemIds = itemOrderDetailsTotal.stream().map(ItemOrderDetailVo::getItemId).distinct().collect(Collectors.toList());
        List<Integer> mallSkuIds = itemOrderDetails4RCZX.stream().map(ItemOrderDetailVo::getMallSkuId).distinct().collect(Collectors.toList());
        // 查询商品图片
        ApiResponse<List<MallItemPic>> mallItemPicResponse = mallItemPicServiceFacade.getMallItemPicByMallItemIds(mallItemIds);
        List<MallItemPic> mallItemPics = mallItemPicResponse.getBody();
        Map<Integer, List<MallItemPic>> mallItemPicMap = mallItemPics.stream().collect(Collectors.groupingBy(MallItemPic::getMallItemId));
        // 查询商品SKU图片
        Map<Integer, List<MallSkuPic>> mallSkuPicMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(mallSkuIds)) {
            ApiResponse<List<MallSkuPic>> mallSkuPicResponse = mallSkuPicServiceFacade.queryMallSkuPicByMallSkuIds(mallSkuIds);
            List<MallSkuPic> mallSkuPics = mallSkuPicResponse.getBody();
            mallSkuPicMap = mallSkuPics.stream().collect(Collectors.groupingBy(MallSkuPic::getMallSkuId));
        }
        // 查询人才中心SKU信息
        Map<Integer, MallSkuExtTalent> mallSkuExtTalentMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(mallSkuIds)){
            ApiResponse<List<MallSkuExtTalent>> mallSkuExtTalentsResponse = mallSkuExtTalentServiceFacade.queryMallSkuExtTalentByMallSkuIds(mallSkuIds);
            List<MallSkuExtTalent> mallSkuExtTalents = mallSkuExtTalentsResponse.getBody();
            mallSkuExtTalentMap = mallSkuExtTalents.stream().collect(Collectors.toMap(MallSkuExtTalent::getMallSkuId, m -> m, (k1, k2) -> k1));
        }
        // 查询校长培训商品信息
        Map<Integer, MallItemExtTrain> mallItemExtTrainMap = new HashMap<>();
        if(existNailOrder) {
            ApiResponse<List<MallItemExtTrain>> mallItemExtTrainResponse = mallItemExtServiceFacade.queryMallItemExtTrainByMallItemIds(mallItemIds);
            List<MallItemExtTrain> mallItemExtTrains = mallItemExtTrainResponse.getBody();
            mallItemExtTrainMap = mallItemExtTrains.stream().collect(Collectors.toMap(MallItemExtTrain::getMallItemId, m -> m, (k1, k2) -> k1));
        }
        MallSkuExtTalent mallSkuExtTalent;
        // 校长培训报名时间格式化
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH:mm");
        for (ItemOrderVo itemOrderVo : itemOrderVos) {
            Integer categoryId = itemOrderVo.getCategoryId();
            for (ItemOrderDetailVo itemOrderDetailVo : itemOrderVo.getItemOrderDetails()) {
                Integer mallSkuId = itemOrderDetailVo.getMallSkuId();
                Integer mallItemId = itemOrderDetailVo.getItemId();
                // 人才中心取SKU图片，其他商品取SPU图片
                if(categoryId.equals(MallItemConstant.Category.RCZX.getId())){
                    itemOrderDetailVo.setPicUrl(mallSkuPicMap.get(mallSkuId).get(0).getPicUrl());
                }else {
                    itemOrderDetailVo.setPicUrl(mallItemPicMap.get(mallItemId).get(0).getPicUrl());
                }
                // 人才中心附加内容
                if(mallSkuExtTalentMap.containsKey(mallSkuId)){
                    mallSkuExtTalent = mallSkuExtTalentMap.get(mallSkuId);
                    itemOrderDetailVo.setEducationRemark(mallSkuExtTalent.getEducationRemark());
                    itemOrderDetailVo.setExperienceRemark(mallSkuExtTalent.getExperienceRemark());
                }
                // 校长培训附加内容
                if(mallItemExtTrainMap.containsKey(mallItemId)){
                    MallItemExtTrain mallItemExtTrain = mallItemExtTrainMap.get(mallItemId);
                    itemOrderDetailVo.setActivityAddress(mallItemExtTrain.getActivityAddress());
                    itemOrderDetailVo.setActivityStartTimeDis(sdf.format(mallItemExtTrain.getActivityStartTime()));
                    itemOrderDetailVo.setActivityEndTimeDis(sdf.format(mallItemExtTrain.getActivityEndTime()));
                }
            }
            if (MallItemConstant.Category.RCZX.getId().equals(categoryId) && isDetail) {
                // 处理fieldValue传ID时的展示文案
                String extInfo = itemOrderVo.getExtInfo();
                List<TalentTemplateVo> talentTemplateVos = JSONObject.parseArray(extInfo, TalentTemplateVo.class);
                String key;
                for (TalentTemplateVo talentTemplateVo : talentTemplateVos) {
                    key = talentTemplateVo.getKey();
                    if (StringUtils.isNotBlank(key)) {
                        talentTemplateVo.setFieldValue(key);
                    }
                }
                itemOrderVo.setExtInfo(JSONObject.toJSONString(talentTemplateVos));
            }
        }
    }
}
