package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.independenceDay.vo.OrderSuccessVo;
import com.gaosi.api.revolver.dto.CancelOrderDto;
import com.gaosi.api.revolver.facade.ExpressServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.ExpressType;
import com.gaosi.api.turing.constant.InstitutionTypeEnum;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.bean.common.Assert;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import com.gaosi.api.vulcan.vo.FreightVo;
import com.gaosi.api.warcraft.mq.TaskProducerApi;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

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

    @Resource
    private InstitutionService institutionService;

    @Resource
    private TaskProducerApi taskProducerApi;

    @Resource
    private ExpressServiceFacade expressServiceFacade;

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
        if (provinceId == null || areaId == null) {
            return ResultData.failed("参数不能为空 ");
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
                UserHandleUtil.getUserId(), consigneeId, receivePhone, express, Arrays.toString(goodsTypeIds), token);
        checkParams4Submit(goodsTypeIds,express);

        Integer userId = UserHandleUtil.getUserId();
        Integer insId = UserHandleUtil.getInsId();
        List<Integer> goodsTypeIdList = Lists.newArrayList(goodsTypeIds);

        OrderSuccessVo orderSuccessVo = orderManager.submit(userId, insId, consigneeId,
                receivePhone, express, goodsTypeIdList, token);

        //发送消息
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("insId", insId);
            map.put("userId", userId);
            map.put("taskCode", "84FA0A9E96C086F232108FA87A711301");
            taskProducerApi.headMasterProducer(map);
        } catch (Exception e) {
            logger.error("创建订单后,发送消息失败", e);
        }

        return ResultData.successed(orderSuccessVo);
    }

    /**
     * 确认收货
     *
     * @param orderId 订单号
     * @return 订单号，用于前端修改指定订单的状态
     */
    @RequestMapping(value = "/receive", method = RequestMethod.POST)
    public ResultData receive(@RequestParam String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return ResultData.failed("参数不能为空");
        }
        orderServiceFacade.updateOrderToComplete(orderId);
        Map<String, Object> map = new HashMap<>(1);
        map.put("orderId", orderId);
        return ResultData.successed(map);
    }

    /**
     * 取消订单
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public ResultData cancelGoodsOrder(@RequestParam String orderId) {
        Integer userId = UserHandleUtil.getUserId();
        logger.info("User [{}] cancel the order [{}]", userId, orderId);
        if (StringUtils.isBlank(orderId)) {
            return ResultData.failed("参数不能为空");
        }
        orderServiceFacade.cancelOrder(orderId,userId);
        return ResultData.successed(orderId);
    }

    /*
     * 验证是否试用机构
     */
    private void validateInsType() {
        Integer insId = UserHandleUtil.getInsId();
        Institution institution = institutionService.getInsInfoById(insId);
        if (InstitutionTypeEnum.TRY.getType() == institution.getInstitutionType()) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "当前机构试用状态，不能下单。");
        }
    }


    private void checkParams4Submit(Integer[] goodsTypeIds, String express) {
        Assert.isTrue(null != goodsTypeIds && goodsTypeIds.length != 0, "所选商品不能为空");
        ApiResponse<List<ExpressType>> expressTypeResponse = expressServiceFacade.queryAllExpressType();
        Map<String, ExpressType> expressTypeMap = CollectionCommonUtil.toMapByList(expressTypeResponse.getBody(), "getCode", String.class);
        Assert.isTrue(expressTypeMap.containsKey(express), "配送方式参数错误");
        Assert.isTrue(expressTypeMap.get(express).getEnable(), "该配送方式停止承运,暂不接单");
        validateInsType(); // 试用机构不能下单
    }
}

