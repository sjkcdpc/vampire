package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.DictionaryManager;
import com.aixuexi.vampire.manager.ItemOrderManager;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.CalculateUtil;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.revolver.dto.QueryItemOrderDto;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.model.ItemOrder;
import com.gaosi.api.revolver.model.OrderDetail;
import com.gaosi.api.revolver.util.ConstantsUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallCategoryServiceFacade;
import com.gaosi.api.vulcan.facade.NailOrderServiceFacade;
import com.gaosi.api.vulcan.model.MallCategory;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.CategoryVo;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.NailOrderVo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

/**
 * @Description: 订单管理
 * @Author: liuxinyun
 * @Date: 2017/8/9 14:14
 */
@RestController
@RequestMapping(value = "/itemOrder")
public class ItemOrderController {

    private final Logger logger = LoggerFactory.getLogger(ItemOrderController.class);

    @Resource(name = "itemOrderManager")
    private ItemOrderManager itemOrderManager;

    @Resource(name = "orderManager")
    private OrderManager orderManager;

    @Autowired
    private FinancialAccountService finAccService;

    @Autowired
    private OrderServiceFacade orderServiceFacade;

    @Autowired
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Autowired
    private NailOrderServiceFacade nailOrderServiceFacade;

    @Autowired
    private MallCategoryServiceFacade mallCategoryServiceFacade;

    @Resource(name = "dictionaryManager")
    private DictionaryManager dictionaryManager;

    @Resource
    private BaseMapper baseMapper;

    @Value("#{${pay_total_time}}")
    private Long payTotal;


    /**
     * 根据条件查询我的订单列表
     * @param queryItemOrderDto
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResultData list(QueryItemOrderDto queryItemOrderDto){
        if (queryItemOrderDto == null){
            return ResultData.failed("查询参数不能为空");
        }
        queryItemOrderDto.setInstitutionId(UserHandleUtil.getInsId());
        queryItemOrderDto.setUserId(UserHandleUtil.getUserId());
        if (queryItemOrderDto.getEndTime() != null){
            Date endTime = queryItemOrderDto.getEndTime();
            endTime = new DateTime(endTime).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            queryItemOrderDto.setEndTime(endTime);
        }
        if (queryItemOrderDto.getCategoryId()==null || queryItemOrderDto.getCategoryId()==0){
            //默认加载教材订单列表
            queryItemOrderDto.setCategoryId(MallItemConstant.Category.JCZB.getId());
        }
        if (queryItemOrderDto.getCategoryId().equals(MallItemConstant.Category.LDPXSC.getId())){
            return queryLDPXSC(queryItemOrderDto);
        }
        if (queryItemOrderDto.getCategoryId().equals(MallItemConstant.Category.JCZB.getId())){
            return queryJCZB(queryItemOrderDto);
        }
        return ResultData.failed("参数类型错误");
    }

    /**
     * 查询教材周边订单列表
     * @param queryItemOrderDto
     * @return
     */
    private ResultData queryJCZB(QueryItemOrderDto queryItemOrderDto){
        ApiResponse<Page<GoodsOrder>> apiResponse = orderServiceFacade.queryGoodsOrder4Client(queryItemOrderDto);
        //响应错误直接返回
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            return ResultData.failed(apiResponse.getMessage());
        }
        //查询成功
        Page<GoodsOrder> page = apiResponse.getBody();
        List<GoodsOrder> goodsOrders = page.getList();
        Page<GoodsOrderVo> retPage = new Page<>();
        retPage.setPageTotal(page.getPageTotal());
        retPage.setPageSize(page.getPageSize());
        retPage.setPageNum(page.getPageNum());
        retPage.setItemTotal(page.getItemTotal());
        retPage.setStartNum(page.getStartNum());
        //总数为0就不进行其他操作了
        if (retPage.getItemTotal() == 0){
            return ResultData.successed(retPage);
        }
        Map<String, String> expressMap = dictionaryManager.selectDictMapByType(Constants.DELIVERY_COMPANY_DICT_TYPE);
        for (GoodsOrder goodsOrder : goodsOrders) {
            String express = expressMap.get(goodsOrder.getExpressCode());
            goodsOrder.setExpressCode(express == null ? "未知发货服务" : express);
        }
        List<GoodsOrderVo> goodsOrderVos = baseMapper.mapAsList(goodsOrders,GoodsOrderVo.class);
        dealGoodsOrderList(goodsOrderVos);
        retPage.setList(goodsOrderVos);
        return ResultData.successed(retPage);
    }

    /**
     * 查询校长培训订单列表
     * @param queryItemOrderDto
     * @return
     */
    private ResultData queryLDPXSC(QueryItemOrderDto queryItemOrderDto){
        ApiResponse<Page<ItemOrderVo>> apiResponse = itemOrderServiceFacade.queryItemOrder4Client(queryItemOrderDto);
        //响应错误直接返回
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            return ResultData.failed(apiResponse.getMessage());
        }
        //查询成功
        Page<ItemOrderVo> page = apiResponse.getBody();
        //总数为0就不进行其他操作了
        if (page.getItemTotal() == 0){
            return ResultData.successed(page);
        }
        List<ItemOrderVo> itemOrderVos = page.getList();
        List<Integer> categoryIds = new ArrayList<>();
        for (ItemOrderVo itemOrderVo : itemOrderVos){
            categoryIds.add(itemOrderVo.getCategoryId());
        }
        ApiResponse<List<MallCategory>> mallCategoryByIds = mallCategoryServiceFacade.findMallCategoryByIds(categoryIds);
        //响应错误直接返回
        if (mallCategoryByIds.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            return ResultData.failed(mallCategoryByIds.getMessage());
        }
        List<MallCategory> mallCategories = mallCategoryByIds.getBody();
        Map<Integer, MallCategory> map = CollectionCommonUtil.toMapByList(mallCategories, "getId", Integer.class);
        for (ItemOrderVo itemOrderVo : itemOrderVos){
            String categoryName = map.get(itemOrderVo.getCategoryId()).getName();
            itemOrderVo.setCategoryName(categoryName);
            //待支付剩余时间，单位秒
            long creatTime = itemOrderVo.getCreateTime().getTime();
            long current = System.currentTimeMillis();
            long payRemainTime = (payTotal-(current-creatTime))/1000;
            payRemainTime = payRemainTime>0 ? payRemainTime : 0;
            itemOrderVo.setPayRemainTime(payRemainTime);
        }
        page.setList(itemOrderVos);
        return ResultData.successed(page);
    }



    /**
     * 提交订单
     * @param itemId
     * @param itemCount
     * @return
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public ResultData submit(@RequestParam Integer itemId, @RequestParam Integer itemCount){
        logger.info("userId=[{}] submit order, itemId=[{}], itemCount=[{}].", UserSessionHandler.getId(), itemId, itemCount);
        if (itemId == null || itemCount == null){
            return ResultData.failed("参数错误");
        }
        //根据商品id查询商品
        ApiResponse<NailOrderVo> voApiResponse = nailOrderServiceFacade.queryMallItemNail(itemId);
        if (voApiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE){
            return ResultData.failed(voApiResponse.getMessage());
        }
        if (voApiResponse.getBody() == null){
            return ResultData.failed("未查询到该校长培训!");
        }
        NailOrderVo nailOrderVo = voApiResponse.getBody();
        if (nailOrderVo.getShelvesStatus() == MallItemConstant.ShelvesStatus.OFF){
            return ResultData.failed("该校长培训已下架!");
        }
        if (nailOrderVo.getSignUpStatus() == MallItemConstant.SignUpStatus.NO_START){
            return ResultData.failed("报名未开始!");
        }
        if (nailOrderVo.getSignUpStatus() == MallItemConstant.SignUpStatus.FINISHED){
            return ResultData.failed("报名已结束!");
        }
        //已报名数量查询
        ApiResponse<List<ItemOrderStatisVo>> apiResponse = itemOrderServiceFacade.getCountByItemId(Lists.newArrayList(itemId));
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE){
            return ResultData.failed("查询已报名数量失败");
        }
        //仅在限制报名人数时做判断
        if(nailOrderVo.getSignUpNum()>0){
            List<ItemOrderStatisVo> itemOrderStatisVos = apiResponse.getBody();
            Map<Integer, ItemOrderStatisVo> map = CollectionCommonUtil.toMapByList(itemOrderStatisVos, "getItemId", Integer.class);
            int signedUpNum = map.get(itemId).getSignedTotal();
            //计算剩余数量
            int remain = nailOrderVo.getSignUpNum() - signedUpNum;
            if (remain < itemCount){
                int overCount = itemCount - remain;
                return ResultData.failed("超过报名数量:"+overCount);
            }
        }

        String orderId = itemOrderManager.submit(nailOrderVo, itemCount, UserHandleUtil.getInsId());
        return ResultData.successed(orderId);
    }

    /**
     * 订单支付
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public ResultData pay(@RequestParam String orderId, @RequestParam String token){
        logger.info("userId=[{}] pay order, orderId=[{}], token=[{}].", UserSessionHandler.getId(), orderId, token);
        if (StringUtils.isBlank(orderId)){
            return ResultData.failed("订单号为空");
        }
        if (StringUtils.isBlank(token)){
            return ResultData.failed("token为空");
        }
        itemOrderManager.pay(orderId, token);
        return ResultData.successed(orderId);
    }

    /**
     * 获取类型和订单状态
     * @return
     */
    @RequestMapping(value = "/getTypeStatus", method = RequestMethod.GET)
    public ResultData getTypeStatus(){
        Map<String, Object> map = new HashMap<>();
        ApiResponse<List<MallCategory>> apiResponse = mallCategoryServiceFacade.findMallCategoryByLevel(1);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE){
            return ResultData.failed("获取商品类型失败" + apiResponse.getMessage());
        }
        List<MallCategory> mallCategories = apiResponse.getBody();
        List<CategoryVo> categoryVos = baseMapper.mapAsList(mallCategories, CategoryVo.class);
        map.put("category", categoryVos);
        map.put("status", ConstantsUtil.getOrderStatusVos());
        return ResultData.successed(map);
    }

    /**
     * 获取付款token
     * @return
     */
    @RequestMapping(value = "/getToken", method = RequestMethod.GET)
    public ResultData getToken(){
        String token = finAccService.getTokenForFinancial();
        return ResultData.successed(token);
    }

    /**
     * 我的订单列表点支付需要
     * @return
     */
    @RequestMapping(value = "/getAmount", method = RequestMethod.GET)
    public ResultData getAmount(@RequestParam String orderId){
        //查询当前机构账号余额
        RemainResult rr = finAccService.getRemainByInsId(UserHandleUtil.getInsId());
        if(rr == null) {
            return ResultData.failed("账户不存在");
        }
        ItemOrder itemOrder = itemOrderManager.getOrderByOrderId(orderId);
        AmountVo amountVo = new AmountVo();
        Double remainAmount = rr.getUsableRemain().doubleValue() / 10000;
        amountVo.setRemainAmount(remainAmount);
        amountVo.setConsumeCount(itemOrder.getConsumeCount());
        return ResultData.successed(amountVo);
    }

    /**
     * 计算总件数/总金额等信息
     *
     * @param goodsOrderVos
     */
    private void dealGoodsOrderList(List<GoodsOrderVo> goodsOrderVos) {
        if (CollectionUtils.isNotEmpty(goodsOrderVos)) {
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                dealGoodsOrder(goodsOrderVo);
            }
        }
    }

    /**
     * 计算订单总件数/总金额等信息
     * @param goodsOrderVo
     */
    private void dealGoodsOrder(GoodsOrderVo goodsOrderVo){
        //订单商品所属类型
        goodsOrderVo.setCategoryId(MallItemConstant.Category.JCZB.getId());
        goodsOrderVo.setCategoryName(MallItemConstant.Category.JCZB.getName());
        // 订单总金额
        goodsOrderVo.setPayAmount(CalculateUtil.add(goodsOrderVo.getConsumeAmount(), goodsOrderVo.getFreight()));
        //商品总件数
        int goodsPieces = 0;
        List<Integer> goodsTypeIds = Lists.newArrayList();
        for (OrderDetail orderDetail : goodsOrderVo.getOrderDetails()) {
            goodsPieces += orderDetail.getNum();
            if (orderDetail.getGoodTypeId() != null) goodsTypeIds.add(orderDetail.getGoodTypeId());
        }
        Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap = orderManager.findGoodsByTypeIds(goodsTypeIds);
        for (OrderDetailVo orderDetailVo : goodsOrderVo.getOrderDetailVos()) {
            ConfirmGoodsVo confirmGoodsVo = confirmGoodsVoMap.get(orderDetailVo.getGoodTypeId());
            orderDetailVo.setWeight(confirmGoodsVo == null ? 0 : confirmGoodsVo.getWeight());
            orderDetailVo.setTotal(CalculateUtil.mul(orderDetailVo.getPrice(), orderDetailVo.getNum().doubleValue()));
        }
        goodsOrderVo.setGoodsPieces(goodsPieces);
    }

}