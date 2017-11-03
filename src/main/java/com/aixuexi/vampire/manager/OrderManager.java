package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.vampire.exception.BusinessException;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.CalculateUtil;
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
import com.gaosi.api.revolver.constant.ExpressConstant;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.OrderDetail;
import com.gaosi.api.revolver.util.ExpressCodeUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.vulcan.constant.GoodsConstant;
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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource
    private ExpressUtil expressUtil;

    @Resource
    private BaseMapper baseMapper;

    @Resource(name = "dictionaryManager")
    private DictionaryManager dictionaryManager;

    @Resource
    private GoodsPicServiceFacade goodsPicServiceFacade;

    @Resource
    private GoodsPeriodServiceFacade goodsPeriodServiceFacade;

    private final static Map<Integer, Integer> periodMap = new HashMap<>();//学期映射

    static {
        periodMap.put(1, 8);//春 1000
        periodMap.put(2, 4);//暑 0100
        periodMap.put(3, 2);//秋 0010
        periodMap.put(4, 1);//寒 0001
    }

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
            if (consigneeVo.getSystemDefault() == true) {
                confirmOrderVo.setDefCneeId(consigneeVo.getId());
                break;
            }
        }
        confirmOrderVo.setConsignees(consigneeVos);
        // 2. 快递公司
        confirmOrderVo.setExpress(expressUtil.getExpress());
        // 3. 用户购物车中商品清单
        List<ShoppingCartList> shoppingCartLists = shoppingCartServiceFacade.queryShoppingCartDetail(userId);
        if (CollectionUtils.isEmpty(shoppingCartLists)) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
        }
        List<Integer> goodsTypeIds = Lists.newArrayList();
        int goodsPieces = 0; // 商品总件数
        double goodsAmount = 0; // 总金额
        double weight = 0; // 商品重量
        // 数量 goodsTypeIds -> num
        Map<Integer, Integer> goodsNum = Maps.newHashMap();
        for (ShoppingCartList shoppingCartList : shoppingCartLists) {
            Integer goodsTypeId = shoppingCartList.getGoodsTypeId();
            Integer num = shoppingCartList.getNum();

            goodsTypeIds.add(goodsTypeId);
            goodsNum.put(goodsTypeId, num);

        }
        // 4. 根据goodsTypeIds查询商品其他信息
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
        for (ConfirmGoodsVo goodsVo : goodsVos) {
            Integer goodsTypeId = goodsVo.getGoodsTypeId();
            Integer typeNum = goodsNum.get(goodsTypeId);
            goodsVo.setNum(typeNum);
            int num = goodsVo.getNum();
            // 数量*单价
            goodsVo.setTotal(num * goodsVo.getPrice());
            if (goodsVo.getStatus() == GoodsConstant.Status.ON) { // 上架
                // 数量*单重量
                weight += num * goodsVo.getWeight();
                goodsAmount += goodsVo.getTotal();
                goodsPieces += num;
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
        if (rr == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        Long remain = rr.getUsableRemain();
        confirmOrderVo.setBalance(Double.valueOf(remain) / 10000);
        // 6. 获取token
        confirmOrderVo.setToken(finAccService.getTokenForFinancial());
        logger.info("confirmOrder end --> confirmOrderVo : {}", confirmOrderVo);
        // 清空之前计算的邮费
        defaultExpressForConfirmOrder(confirmOrderVo.getExpress(), goodsPieces);
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
        if (rr == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        if (CollectionUtils.isEmpty(goodsTypeIds)) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "所选商品不能为空");
        }
        List<ShoppingCartList> shoppingCartLists = shoppingCartServiceFacade.queryShoppingCartDetail(userId, goodsTypeIds);
        if (CollectionUtils.isEmpty(shoppingCartLists)) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
        }

        // 创建订单对象
        GoodsOrderVo goodsOrder = createGoodsOrder(shoppingCartLists, userId, insId, consigneeId, receivePhone, express);
        logger.info("submitOrder --> goodsOrder info : {}", JSONObject.toJSONString(goodsOrder));
        // 支付金额 = 商品金额 + 邮费
        Double amount = (goodsOrder.getConsumeAmount() + goodsOrder.getFreight()) * 10000;
        Long remain = rr.getUsableRemain();
        if (amount.longValue() > remain) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "余额不足");
        }
        // 是否走发网
        Boolean syncToWms = true;
        // ruanyj 测试环境是否发网的开关
        Institution insinfo = institutionService.getInsInfoById(insId);
        // 1测试机构 2试用机构 或者关闭发网开关 则不走发网
        if ((insinfo != null && Constants.INS_TYPES.contains(insinfo.getInstitutionType().intValue()))
                || !expressUtil.getSyncToWms()) {
            syncToWms = false;
        }
        logger.info("submitOrder --> syncToWms : {}", syncToWms);
        // 创建订单
        ApiResponse<SimpleGoodsOrderVo> apiResponse = orderServiceFacade.createOrder(goodsOrder, token, syncToWms);
        if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
            SimpleGoodsOrderVo simpleGoodsOrderVo = apiResponse.getBody();
            logger.info("submitOrder --> simpleGoodsOrderVo : {}", simpleGoodsOrderVo);
            List<Integer> shoppingCartListIds = Lists.newArrayList();
            for (ShoppingCartList shoppingCartList : shoppingCartLists) {
                shoppingCartListIds.add(shoppingCartList.getId());
            }
            shoppingCartServiceFacade.clearShoppingCart(shoppingCartListIds);
            return new OrderSuccessVo(simpleGoodsOrderVo.getOrderId(), getTipsByExpress(express), getSplitTips(simpleGoodsOrderVo.getSplitNum()));
        } else {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
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
    private GoodsOrderVo createGoodsOrder(List<ShoppingCartList> shoppingCartLists, Integer userId, Integer insId,
                                          Integer consigneeId, String receivePhone, String express) {
        // 收货人信息判断
        Consignee consignee = consigneeServiceFacade.selectById(consigneeId);
        if (consignee == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "请选择收货地址");
        }
        // 订单
        GoodsOrderVo goodsOrder = new GoodsOrderVo();
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
            Integer goodsTypeId = shoppingCartList.getGoodsTypeId();
            Integer num = shoppingCartList.getNum();
            goodsTypeIds.add(goodsTypeId);
            goodsNum.put(goodsTypeId, num);
            goodsPieces += num;
        }
        // 订单详情
        List<OrderDetailVo> orderDetails = Lists.newArrayList();
        // 查询商品明细
        ApiResponse<List<ConfirmGoodsVo>> listApiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        if (listApiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, listApiResponse.getMessage());
        }
        if (CollectionUtils.isEmpty(listApiResponse.getBody())) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "商品不存在! ");
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
            OrderDetailVo orderDetail = new OrderDetailVo();
            orderDetail.setBarCode(confirmGoodsVo.getBarCode());
            orderDetail.setGoodsId(confirmGoodsVo.getGoodsId());
            orderDetail.setGoodTypeId(confirmGoodsVo.getGoodsTypeId());
            orderDetail.setName(confirmGoodsVo.getGoodsName() + Constants.ORDERDETAIL_NAME_DIV + confirmGoodsVo.getGoodsTypeName());
            orderDetail.setNum(num);
            orderDetail.setPrice(confirmGoodsVo.getPrice());
            orderDetails.add(orderDetail);
        }
        handlePeriod(orderDetails);
        goodsOrder.setOrderDetailVos(orderDetails);
        goodsOrder.setAreaId(consignee.getAreaId());
        ApiResponse<List<AddressDTO>> ad = areaApi.findAddressByIds(consignee.getAreaId());
        goodsOrder.setConsigneeName(consignee.getName());
        goodsOrder.setConsigneePhone(consignee.getPhone());
        //ruanyj 收货人地址补全
        AddressDTO add = ad.getBody().get(0);
        StringBuilder preAddress = new StringBuilder();
        if (add != null) {
            preAddress.append(add.getProvince() == null ? "" : add.getProvince());
            preAddress.append(add.getCity() == null ? "" : add.getCity());
            preAddress.append(add.getDistrict() == null ? "" : add.getDistrict());
        }
        if (StringUtils.isBlank(preAddress.toString())) {
            goodsOrder.setConsigneeAddress(consignee.getAddress());
        } else {
            goodsOrder.setConsigneeAddress(preAddress.toString() + " " + consignee.getAddress());
        }
        goodsOrder.setConsumeAmount(goodsAmount); // 商品总金额
        goodsOrder.setInstitutionId(insId);
        goodsOrder.setRemark(StringUtils.EMPTY);
        goodsOrder.setReceivePhone(StringUtils.isBlank(receivePhone) ? consignee.getPhone() : receivePhone); // 发货通知手机号
        goodsOrder.setUserId(userId);
        ApiResponse<List<HashMap<String, Object>>> apiResponse;
        boolean isFree = false; // 是否免物流费
        if (express.equals(OrderConstant.LogisticsMode.EXPRESS_DBWL) && goodsPieces >= 100) {
            goodsOrder.setExpressCode(OrderConstant.LogisticsMode.EXPRESS_DBWL);
            isFree = true;
        } else {
            goodsOrder.setExpressCode(express);
        }
        //设置配送方式
        goodsOrder.setExpressType(ExpressConstant.Express.getIdByCode(express).toString());
        // 是否计算邮费
        if (isFree) {
            goodsOrder.setFreight(0D);
        } else {
            List<AddressDTO> addressDTOS = findAddressByIds(consignee.getAreaId());
            if (!CollectionUtils.isEmpty(addressDTOS)) {
                // 省ID
                Integer provinceId = addressDTOS.get(0).getProvinceId();
                apiResponse = orderServiceFacade.newCalFreight(provinceId, weight, express, goodsPieces);
                if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
                    throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
                }
                HashMap<String, Object> freightMap = apiResponse.getBody().get(0);
                goodsOrder.setFreight(Double.valueOf(freightMap.get("totalFreight").toString()));
            }
        }
        //TODO判断是否拆单
        goodsOrder.setSplitNum(1);
        return goodsOrder;
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

        for (OrderDetailVo orderDetailVo : orderDetailVos) {
            List<GoodsPeriod> goodsPeriods = goodsPeriodMap.get(orderDetailVo.getGoodsId());
            Integer allPeriod = 0;
            if (CollectionUtils.isNotEmpty(goodsPeriods)) {
                try {
                    DateTime now = new DateTime();
                    for (GoodsPeriod goodsPeriod : goodsPeriods) {
                        if (StringUtils.isNotBlank(goodsPeriod.getStartTime())) {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MM月dd日");
                            DateTime startTime = DateTime.parse(goodsPeriod.getStartTime(), dateTimeFormatter);
                            DateTime endTime = DateTime.parse(goodsPeriod.getEndTime(), dateTimeFormatter);
                            //将年份设置为当年（忽略年份对判断的影响）
                            startTime = startTime.withYear(now.getYear());
                            endTime = endTime.withYear(now.getYear()).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
                            if (startTime.isAfter(endTime)) {//开始日期大于结束日期,表示跨年
                                if (startTime.isBeforeNow() || endTime.isAfterNow()) {//如果满足其实任一条件
                                    Integer period = goodsPeriod.getPeriod();
                                    allPeriod = allPeriod | periodMap.get(period);
                                }
                            } else {
                                if (startTime.isBeforeNow() && endTime.isAfterNow()) {//如果在该学期设定的时间段中
                                    Integer period = goodsPeriod.getPeriod();
                                    allPeriod = allPeriod | periodMap.get(period);
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
        if (apiResponse != null) {
            if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
                if (CollectionUtils.isEmpty(apiResponse.getBody())) {
                    throw new BusinessException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
                }
                return apiResponse.getBody();
            } else {
                throw new BusinessException(ExceptionCode.UNKNOWN, "获取收货地址市异常");
            }
        } else {
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
        ApiResponse<List<HashMap<String, Object>>> apiResponse;
//        Date curDate = new Date();
//        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//小写的mm表示的是分钟
//        Date updateTime= null;
//        try {
//            updateTime = sdf.parse(expressUtil.getFreightUpdateTime());
//        }
//        catch (ParseException e) {
//            logger.error("updateTime : {} ParseException {} ",expressUtil.getFreightUpdateTime(),e.getMessage());
//        }
//        Boolean flag = curDate.before(updateTime);
//        if(flag) {
//            apiResponse = orderServiceFacade.calculateFreight(provinceId, weight, OrderConstant.LogisticsMode.EXPRESS,goodsPieces);
//        }
//        else{
        apiResponse = orderServiceFacade.newCalFreight(provinceId, weight, OrderConstant.LogisticsMode.EXPRESS, goodsPieces);
//        }
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        List<HashMap<String, Object>> listMap = apiResponse.getBody();
        for (int i = 0; i < expressVoLists.size(); i++) {
            ConfirmExpressVo confirmExpressVo = expressVoLists.get(i);
            HashMap<String, Object> map = listMap.get(i);
            confirmExpressVo.setFirstFreight(map.get("firstFreight").toString());
            confirmExpressVo.setBeyondPrice(map.get("beyondPrice").toString());
            confirmExpressVo.setBeyondWeight(map.get("beyondWeight").toString());
            confirmExpressVo.setTotalFreight(map.get("totalFreight").toString());
            confirmExpressVo.setRemark(map.get("remark").toString());
            if (confirmExpressVo.getKey().equals(OrderConstant.LogisticsMode.EXPRESS_DBWL)) {
//                if(flag) {
//                        confirmExpressVo.setDesc(OrderConstant.LogisticsMode.DESC);
//                }
//                else {
                String desc = (goodsPieces >= 100) ? OrderConstant.LogisticsMode.NEW_FREE_DESC : OrderConstant.LogisticsMode.NEW_NOT_SUP_DESC;
                confirmExpressVo.setDesc(desc);
//                }
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
                throw new BusinessException(ExceptionCode.UNKNOWN, "购物车中商品已结算或为空");
            }
            goodsTypeIds = Lists.newArrayList();

            // 数量 goodsTypeIds - > num
            Map<Integer, Integer> goodsNum = Maps.newHashMap();
            for (ShoppingCartList shoppingCartList : shoppingCartLists) {
                Integer goodsTypeId = shoppingCartList.getGoodsTypeId();
                Integer num = shoppingCartList.getNum();

                goodsTypeIds.add(goodsTypeId);
                goodsNum.put(goodsTypeId, num);
            }

            ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
            if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
                throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
            }
            List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
            if (CollectionUtils.isEmpty(goodsVos)) {
                throw new BusinessException(ExceptionCode.UNKNOWN, "商品不存在! ");
            }
            // List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
            for (ConfirmGoodsVo goodsVo : goodsVos) {
                if (goodsVo.getStatus() == GoodsConstant.Status.ON) { // 上架
                    Integer goodsTypeId = goodsVo.getGoodsTypeId();
                    Integer goodsNumValue = goodsNum.get(goodsTypeId);
                    Double unitWeight = goodsVo.getWeight();
                    // 数量*单重量
                    weight += goodsNumValue * unitWeight;

                    Double price = goodsVo.getPrice();
                    // 数量*单价
                    goodsAmount += goodsNumValue * price;
                    goodsPieces += goodsNumValue;
                }
            }
        }
        // 计算邮费
        calcFreight(provinceId, weight, confirmExpressVos, goodsPieces);
        // set
        freightVo.setGoodsPieces(goodsPieces);
        freightVo.setGoodsWeight(weight);
        freightVo.setGoodsAmount(goodsAmount);
        freightVo.setExpress(confirmExpressVos);
        // 账号余额
        RemainResult rr = finAccService.getRemainByInsId(insId);
        if (rr == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "账户不存在");
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
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(goodsTypeIds);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        List<ConfirmGoodsVo> goodsVos = apiResponse.getBody();
        Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(goodsVos)) {
            confirmGoodsVoMap = CollectionCommonUtil.toMapByList(goodsVos, "getGoodsTypeId", Integer.class);
        }
        return confirmGoodsVoMap;
    }

    /**
     * 根据商品id查询图片
     * @param goodsIds
     * @return
     */
    public Map<Integer, List<GoodsPic>> getPicByGoodsIds(List<Integer> goodsIds) {
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
     * 根据不同的express提示信息
     *
     * @param expressCode
     * @return
     */
    private String getTipsByExpress(String expressCode) {
        Map<String, ExpressVo> expressVoMap = CollectionCommonUtil.toMapByList(expressUtil.getExpressVos(), "getCode", String.class);
        String tips = expressVoMap.get(expressCode).getTips();
        if (StringUtils.isBlank(tips)) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "请选择发货服务方式");
        }
        return tips;
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
     * 校验库存和是否下架
     *
     * @param confirmGoodsVos
     * @param goodsNum
     */
    private void validateGoods(List<ConfirmGoodsVo> confirmGoodsVos, Map<Integer, Integer> goodsNum) {
        if (!CollectionUtils.isNotEmpty(confirmGoodsVos)) {
            return;
        }

        List<ConfirmGoodsVo> offGoods = Lists.newArrayList();
        List<String> barCodes = Lists.newArrayList();
        // 1. 校验商品是否已经下架
        for (ConfirmGoodsVo confirmGoodsVo : confirmGoodsVos) {
            //ruanyj 商品编码为空校验
            String barCode = confirmGoodsVo.getBarCode();
            if (StringUtils.isBlank(barCode)) {
                throw new BusinessException(ExceptionCode.UNKNOWN, confirmGoodsVo.getGoodsName() + confirmGoodsVo.getGoodsTypeName() + "的SKU编码为空!");
            }
            barCodes.add(confirmGoodsVo.getBarCode());
            if (confirmGoodsVo.getStatus() == GoodsConstant.Status.OFF) {
                offGoods.add(confirmGoodsVo);
            }
        }
        if (CollectionUtils.isNotEmpty(offGoods)) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "存在已下架商品!");
        }
        if (expressUtil.getIsInventory()) {
            // 2. 校验库存 {barCode, inventory}
            boolean flag = false;
            ApiResponse<Map<String, Integer>> apiResponse = orderServiceFacade.queryInventory(barCodes);
            // ruanyj 查询库存校验
            if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
                throw new BusinessException(ExceptionCode.UNKNOWN, "查询库存失败!");
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

    /**
     * 处理完成，每次需要清空
     *
     * @param expressVos
     */
    private void defaultExpressForConfirmOrder(List<ConfirmExpressVo> expressVos, Integer goodsPieces) {

//        Date curDate = new Date();
//        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//小写的mm表示的是分钟
//        Date updateTime= null;
//        try {
//            updateTime = sdf.parse(expressUtil.getFreightUpdateTime());
//        } catch (ParseException e) {
//            logger.error("updateTime : {} ParseException {} ",expressUtil.getFreightUpdateTime(),e.getMessage());
//        }
        for (ConfirmExpressVo expressVo : expressVos) {
            expressVo.setBeyondPrice(StringUtils.EMPTY);
            expressVo.setBeyondWeight(StringUtils.EMPTY);
            expressVo.setFirstFreight(StringUtils.EMPTY);
            expressVo.setTotalFreight(StringUtils.EMPTY);
            expressVo.setRemark(StringUtils.EMPTY);
            if (expressVo.getKey().equals(OrderConstant.LogisticsMode.EXPRESS_DBWL)) {
//                if(curDate.before(updateTime)) {
//                    expressVo.setDesc(OrderConstant.LogisticsMode.DESC);
//                }
//                else {
                String desc = (goodsPieces >= 100) ? OrderConstant.LogisticsMode.NEW_FREE_DESC : OrderConstant.LogisticsMode.NEW_NOT_SUP_DESC;
                expressVo.setDesc(desc);
//                }
            }
        }
    }

    /**
     * 处理多个订单使用
     * @param goodsOrderVos
     * @param needDetail 是否需要图等详细信息
     */
    public void dealGoodsOrderVos(List<GoodsOrderVo> goodsOrderVos, boolean needDetail){
        if (CollectionUtils.isNotEmpty(goodsOrderVos)) {
            Set<Integer> goodsTypeIds = Sets.newHashSet();
            Set<Integer> goodsIds = Sets.newHashSet();
            // 准备工作,防止多次调用微服务
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                if (goodsOrderVo.getSplitNum() > 1){
                    // 拆单的处理子订单
                    List<SubGoodsOrderVo> subGoodsOrderVos = goodsOrderVo.getSubGoodsOrderVos();
                    for (SubGoodsOrderVo subGoodsOrderVo : subGoodsOrderVos) {
                        for (SubOrderDetailVo subOrderDetailVo : subGoodsOrderVo.getSubOrderDetailVos()) {
                            goodsTypeIds.add(subOrderDetailVo.getGoodTypeId());
                            goodsIds.add(subOrderDetailVo.getGoodsId());
                        }
                    }
                }else {
                    for (OrderDetailVo orderDetailVo : goodsOrderVo.getOrderDetailVos()) {
                        goodsTypeIds.add(orderDetailVo.getGoodTypeId());
                        goodsIds.add(orderDetailVo.getGoodsId());
                    }
                }
            }
            Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap = this.findGoodsByTypeIds(new ArrayList<Integer>(goodsTypeIds));
            Map<Integer, List<GoodsPic>> picMap = null;
            if (needDetail) {
                // 订单详情查看需要获取图片
                picMap = this.getPicByGoodsIds(new ArrayList<Integer>(goodsIds));
            }
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                this.dealGoodsOrderVo(goodsOrderVo, confirmGoodsVoMap, picMap);
            }
        }
    }

    /**
     * 计算订单订单详情中的重量/小计等信息
     * @param goodsOrderVo
     * @param confirmGoodsVoMap
     */
    private void dealGoodsOrderVo(GoodsOrderVo goodsOrderVo, Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap, Map<Integer, List<GoodsPic>> picMap){
        //拆单需要处理子订单
        if (goodsOrderVo.getSplitNum() > 1){
            // 拆单的去除父订单多余数据
            goodsOrderVo.getOrderDetailVos().clear();
            for (SubGoodsOrderVo subGoodsOrderVo : goodsOrderVo.getSubGoodsOrderVos()) {
                dealSubGoodsOrderVo(subGoodsOrderVo, confirmGoodsVoMap, picMap);
            }
        }else {
            //详情中的一些信息
            for (OrderDetailVo orderDetailVo : goodsOrderVo.getOrderDetailVos()) {
                ConfirmGoodsVo confirmGoodsVo = confirmGoodsVoMap.get(orderDetailVo.getGoodTypeId());
                orderDetailVo.setWeight(confirmGoodsVo == null ? 0 : confirmGoodsVo.getWeight());
                orderDetailVo.setTotal(CalculateUtil.mul(orderDetailVo.getPrice(), orderDetailVo.getNum().doubleValue()));
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
     */
    private void dealSubGoodsOrderVo(SubGoodsOrderVo subGoodsOrderVo, Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap, Map<Integer, List<GoodsPic>> picMap){
        List<SubOrderDetailVo> subOrderDetailVos = subGoodsOrderVo.getSubOrderDetailVos();
        Double consumeAmount = 0d ;
        //子订单详情中的一些信息
        for (SubOrderDetailVo subOrderDetailVo : subOrderDetailVos) {
            ConfirmGoodsVo confirmGoodsVo = confirmGoodsVoMap.get(subOrderDetailVo.getGoodTypeId());
            subOrderDetailVo.setWeight(confirmGoodsVo == null ? 0 : confirmGoodsVo.getWeight());
            subOrderDetailVo.setTotal(CalculateUtil.mul(subOrderDetailVo.getPrice(), subOrderDetailVo.getNum().doubleValue()));
            consumeAmount = CalculateUtil.add(consumeAmount,subOrderDetailVo.getTotal());
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
