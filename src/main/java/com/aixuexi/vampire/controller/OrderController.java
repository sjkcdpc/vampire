package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.exception.BusinessException;
import com.aixuexi.vampire.manager.DictionaryManager;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.BaseMapper;
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
import com.gaosi.api.revolver.facade.ExpressServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.facade.SubOrderServiceFacade;
import com.gaosi.api.revolver.model.Express;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.revolver.vo.OrderFollowVo;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import com.gaosi.api.vulcan.vo.FreightVo;
import com.gaosi.api.warcraft.mq.TaskProducerApi;
import com.google.common.collect.Lists;
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

    @Resource
    private OrderServiceFacade orderServiceFacade;

    @Resource(name = "dictionaryManager")
    private DictionaryManager dictionaryManager;

    @Resource
    private InstitutionService institutionService;

    @Resource
    private TaskProducerApi taskProducerApi;

    @Resource
    private SubOrderServiceFacade subOrderServiceFacade;

    @Resource
    private ExpressServiceFacade expressServiceFacade;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 订单详情
     *
     * @param orderId 订单号
     * @return
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public ResultData detail(@RequestParam String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return ResultData.failed("参数错误");
        }
        ApiResponse<GoodsOrderVo> apiResponse = orderServiceFacade.getGoodsOrderWithDetailById(orderId);
        //响应错误直接返回
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(apiResponse.getMessage());
        }
        GoodsOrderVo goodsOrderVo = apiResponse.getBody();
        List<GoodsOrderVo> goodsOrderVos = Lists.newArrayList(goodsOrderVo);
        // 订单详情需要加载图片
        orderManager.dealGoodsOrderVos(goodsOrderVos, true);
        return ResultData.successed(goodsOrderVo);
    }

    /**
     * 确认订单
     *
     * @return
     */
    @RequestMapping(value = "/confirm", method = RequestMethod.GET)
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
     * @param provinceId 省id
     * @param areaId 区id
     * @param goodsTypeIds 商品类型id
     * @return
     */
    @RequestMapping(value = "/freight", method = RequestMethod.GET)
    public ResultData freight(@RequestParam Integer provinceId,@RequestParam Integer areaId, Integer[] goodsTypeIds) {
        if (provinceId == null) {
            return ResultData.failed("收货人地址有误! ");
        }
        ResultData resultData = new ResultData();

        Integer userId = UserHandleUtil.getUserId();
        Integer insId = UserHandleUtil.getInsId();
        List<Integer> goodsTypeIdList = (goodsTypeIds == null) ? null : Lists.newArrayList(goodsTypeIds);

        FreightVo freightVo = orderManager.reloadFreight(userId, insId, provinceId,areaId, goodsTypeIdList);
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
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
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
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("insId", UserHandleUtil.getInsId());
            map.put("userId", UserHandleUtil.getUserId());
            map.put("taskCode", "84FA0A9E96C086F232108FA87A711301");
            taskProducerApi.headMasterProducer(map);
        } catch (Exception e) {
            logger.error("创建订单后,发送消息失败", e);
        }

        return resultData;
    }

    /**
     * 确认收货
     *
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/receive", method = RequestMethod.POST)
    public ResultData receive(@RequestParam String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return ResultData.failed("参数不能为空");
        }
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setId(orderId);
        goodsOrder.setStatus(OrderConstant.Status.COMPLETED);
        ApiResponse<?> apiResponse = orderServiceFacade.updateOrder(goodsOrder);
        //响应错误直接返回
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
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
     * 订单跟踪
     *
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/logistics", method = RequestMethod.GET)
    public ResultData getLogisticsData(@RequestParam String orderId) {
        ApiResponse<?> apiResponse;
        if (orderId.contains(OrderConstant.SUB_ORDER_ID_FLAG)) {
            apiResponse = subOrderServiceFacade.getSubGoodsOrderById(orderId);
        } else {
            apiResponse = orderServiceFacade.getGoodsOrderWithDetailById(orderId);
        }
        //查询订单跟踪响应错误
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(apiResponse.getMessage());
        }
        OrderFollowVo orderFollowVo = baseMapper.map(apiResponse.getBody(), OrderFollowVo.class);
        ApiResponse<List<Express>> expressResponse = expressServiceFacade.queryAllExpress();
        //查询快递信息响应错误
        if (expressResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(expressResponse.getMessage());
        }
        Map<String, Express> expressNameMap = CollectionCommonUtil.toMapByList(expressResponse.getBody(),"getCode",String.class) ;
        orderFollowVo.setExpressName(expressNameMap.get(orderFollowVo.getExpressCode()).getName());
        return ResultData.successed(orderFollowVo);
    }

    /*
     * 验证是否试用机构
     */
    private void validateInsType() {
        Integer insId = UserHandleUtil.getInsId();
        Institution institution = institutionService.getInsInfoById(insId);
        if (Constants.INSTITUTION_TYPE_TEST_USE.equals(institution.getInstitutionType())) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "当前机构试用状态，不能下单。");
        }
    }
}

