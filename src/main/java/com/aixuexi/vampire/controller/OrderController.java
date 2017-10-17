package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.manager.DictionaryManager;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.CalculateUtil;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.independenceDay.model.Institution;
import com.gaosi.api.independenceDay.service.InstitutionService;
import com.gaosi.api.independenceDay.vo.OrderSuccessVo;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.model.OrderDetail;
import com.gaosi.api.revolver.util.ExpressCodeUtil;
import com.gaosi.api.revolver.util.JsonUtil;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.revolver.vo.OrderDetailVo;
import com.gaosi.api.vulcan.facade.GoodsPicServiceFacade;
import com.gaosi.api.vulcan.model.GoodsPic;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import com.gaosi.api.vulcan.vo.FreightVo;
import com.gaosi.api.warcraft.mq.TaskProducerApi;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;


/**
 * 订单
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/order")
public class OrderController {
    private final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Resource(name = "orderManager")
    private OrderManager orderManager;

    @Autowired
    private OrderServiceFacade orderServiceFacade;

    @Resource(name = "dictionaryManager")
    private DictionaryManager dictionaryManager;

    @Autowired
    private InstitutionService institutionService;

    @Resource
    private GoodsPicServiceFacade goodsPicServiceFacade;

    @Resource
    private BaseMapper baseMapper;
    @Resource
    private TaskProducerApi taskProducerApi;

    /**
     * 订单详情
     *
     * @param orderId 订单号
     * @return
     */
    @RequestMapping(value = "/detail",method = RequestMethod.GET)
    public ResultData detail(@RequestParam String orderId) {
        if (StringUtils.isBlank(orderId)){
            return ResultData.failed("参数错误");
        }
        ApiResponse<GoodsOrder> apiResponse = orderServiceFacade.getGoodsOrderById(orderId);
        //响应错误直接返回
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            return ResultData.failed(apiResponse.getMessage());
        }
        GoodsOrder goodsOrder = apiResponse.getBody();
        logger.info("detail.GoodsOrder:{}", JsonUtil.objToJson(goodsOrder));
        if(!ExpressCodeUtil.convertExpressCode(goodsOrder)){
            Map<String, String> expressMap = dictionaryManager.selectDictMapByType(Constants.DELIVERY_COMPANY_DICT_TYPE);
            String express = expressMap.get(goodsOrder.getExpressCode());
            goodsOrder.setExpressCode(express == null ? "未知发货服务" : express);
        }
        GoodsOrderVo goodsOrderVo = baseMapper.map(goodsOrder,GoodsOrderVo.class);;
        dealGoodsOrder(goodsOrderVo);
        List<OrderDetailVo> orderDetailVos = goodsOrderVo.getOrderDetailVos();
        addGoodsPics(orderDetailVos);
        return ResultData.successed(goodsOrderVo);
    }
    /**
     * ruanyj 添加商品图片
     */
    private void addGoodsPics(List<OrderDetailVo> orderDetailVos) {
        if (CollectionUtils.isNotEmpty(orderDetailVos)) {
            List<Integer> goodsIds = Lists.newArrayList();
            for (OrderDetailVo orderDetailVo : orderDetailVos) {
                Integer goodsId = orderDetailVo.getGoodsId();
                goodsIds.add(goodsId);
            }
            // goodsId -> goodsPics
            List<GoodsPic> allGoodsPics = goodsPicServiceFacade.findGoodsPicByGoodsIds(goodsIds).getBody();
            Map<Integer, List<GoodsPic>> picMap = CollectionCommonUtil.groupByList(allGoodsPics,"getGoodsId",Integer.class);
            for (OrderDetailVo orderDetailVo : orderDetailVos) {
                Integer goodsId = orderDetailVo.getGoodsId();
                List<GoodsPic> singleGoodsPics = picMap.get(goodsId);
                if(singleGoodsPics!=null){
                    Collections.sort(singleGoodsPics);
                }
                orderDetailVo.setGoodsPics(singleGoodsPics);
            }
        }
    }

    /**
     * 确认订单
     *
     * @return
     */
    @RequestMapping(value = "/confirm",method = RequestMethod.GET)
    public ResultData confirm() {
        ResultData resultData = new ResultData();

        Integer userId = UserHandleUtil.getUserId();
        Integer insId = UserHandleUtil.getInsId();
        ConfirmOrderVo conOrderVo = orderManager.confirmOrder(userId, insId);

        resultData.setBody(conOrderVo);
        return resultData;
    }

    /**
     * 计算运费
     *
     * @param provinceId   省ID
     * @param goodsTypeIds 商品类型ID
     * @return
     */
    @RequestMapping(value = "/freight",method = RequestMethod.GET)
    public ResultData freight(@RequestParam Integer provinceId, Integer[] goodsTypeIds) {
        if(provinceId == null) {
            return ResultData.failed("收货人地址有误! ");
        }
        ResultData resultData = new ResultData();

        Integer userId = UserHandleUtil.getUserId();
        Integer insId = UserHandleUtil.getInsId();
        List<Integer> goodsTypeIdList = (goodsTypeIds == null) ? null : Lists.newArrayList(goodsTypeIds);

        FreightVo freightVo = orderManager.reloadFreight(userId, insId, provinceId, goodsTypeIdList);
        resultData.setBody(freightVo);
        return resultData;
    }

    /**
     * 提交订单
     *
     * @param consigneeId  收货人ID
     * @param receivePhone 接收发货通知手机号
     * @param express      快递
     * @param goodsTypeIds 商品类型ID
     * @param token        财务token
     * @return
     */
    @RequestMapping(value = "/submit",method = RequestMethod.POST)
    public ResultData submit(@RequestParam Integer consigneeId, String receivePhone,
                             @RequestParam String express, Integer[] goodsTypeIds, @RequestParam String token) {
        logger.info("userId=[{}] submit order, consigneeId=[{}], receivePhone=[{}], express=[{}], goodsTypeIds=[{}], token=[{}].",
                UserSessionHandler.getId(), consigneeId, receivePhone, express, Arrays.toString(goodsTypeIds), token);
        ResultData resultData = new ResultData();
        try {
            validateInsType(); // 试用机构不能下单

            Integer userId = UserHandleUtil.getUserId();
            Integer insId = UserHandleUtil.getInsId();
            List<Integer> goodsTypeIdList = goodsTypeIds == null ? null : Lists.newArrayList(goodsTypeIds);

            OrderSuccessVo orderSuccessVo = orderManager.submit(userId, insId, consigneeId,
                    receivePhone, express, goodsTypeIdList, token);

            resultData.setBody(orderSuccessVo);
        } catch (IllegalArgumentException e) {
            //查询库存失败时抛出,前端要求status为normal
            String jsonString = e.getMessage();
            resultData.setBody(JSONObject.parseArray(jsonString, ConfirmGoodsVo.class));
            resultData.setStatus(ResultData.STATUS_NORMAL);
            return resultData;
        }
        //发送消息
        try{
            Map<String, Object> map =new HashMap<>();
            map.put("insId", UserHandleUtil.getInsId());
            map.put("userId", UserHandleUtil.getUserId());
            map.put("taskCode", "84FA0A9E96C086F232108FA87A711301");
            taskProducerApi.headMasterProducer(map);
        }catch (Exception e){
            logger.error("创建订单后,发送消息失败",e);
        }

        return resultData;
    }

    /**
     * 确认收货
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/receive",method = RequestMethod.POST)
    public ResultData receive(@RequestParam String orderId){
        if (StringUtils.isBlank(orderId)){
            return ResultData.failed("参数不能为空");
        }
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setId(orderId);
        goodsOrder.setStatus(OrderConstant.Status.COMPLETED);
        ApiResponse<?> apiResponse = orderServiceFacade.updateOrder(goodsOrder);
        //响应错误直接返回
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            return ResultData.failed(apiResponse.getMessage());
        }
        return ResultData.successed();
    }

    /**
     * 取消订单
     *
     * @return
     */
    @RequestMapping(value = "/cancel")
    public ResultData cancel() {
        // TODO
        return null;
    }

    /**
     * 计算订单总件数/总金额等信息
     * @param goodsOrderVo
     */
    private void dealGoodsOrder(GoodsOrderVo goodsOrderVo){
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
    /**
     * 验证是否试用机构
     */
    private void validateInsType() {
        Integer insId = UserHandleUtil.getInsId();
        Institution institution = institutionService.getInsInfoById(insId);
        if (Constants.INSTITUTION_TYPE_TEST_USE.equals(institution.getInstitutionType())) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "当前机构试用状态，不能下单。");
        }
    }
}

