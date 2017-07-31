package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.DictionaryManager;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.CalculateUtil;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.independenceDay.model.Institution;
import com.gaosi.api.independenceDay.service.InstitutionService;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.revolver.vo.OrderDetailVo;
import com.gaosi.api.vulcan.facade.GoodsPicServiceFacade;
import com.gaosi.api.vulcan.model.GoodsPic;
import com.gaosi.api.vulcan.model.LogisticsData;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import com.gaosi.api.vulcan.vo.FreightVo;
import com.gaosi.api.warcraft.mq.TaskProducerApi;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.DecimalFormat;
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
     * 订单列表
     *
     * @param pageIndex 页号
     * @param pageSize  页码
     * @return
     */
    @RequestMapping(value = "/list")
    public ResultData list(@RequestParam Integer pageIndex, @RequestParam Integer pageSize) {
        ResultData resultData = new ResultData();
        Page<GoodsOrder> page = orderServiceFacade.selectGoodsOrderByIns(UserHandleUtil.getInsId(),
                UserHandleUtil.getUserId(), pageIndex, pageSize);
        Page<GoodsOrderVo> retPage = new Page<GoodsOrderVo>();
        retPage.setPageTotal(page.getPageTotal());
        retPage.setPageSize(page.getPageSize());
        retPage.setPageNum(page.getPageNum());
        retPage.setItemTotal(page.getItemTotal());
        retPage.setStartNum(page.getStartNum());
        Map<String, String> expressMap = dictionaryManager.selectDictMapByType(Constants.DELIVERY_COMPANY_DICT_TYPE);
        for (GoodsOrder goodsOrder : page.getList()) {
            String express = expressMap.get(goodsOrder.getExpressCode());
            goodsOrder.setExpressCode(express == null ? "未知发货服务" : express);
        }
        List<GoodsOrderVo> goodsOrderVos = baseMapper.mapAsList(page.getList(),GoodsOrderVo.class);
        dealGoodsOrder(goodsOrderVos);
        //ruanyj 查询商品图片，放到web层处理，商品图片和订单没有关系
        for(GoodsOrderVo goodsOrderVo:goodsOrderVos) {
            List<OrderDetailVo> orderDetails = goodsOrderVo.getOrderDetails();
            addGoodsPics(orderDetails);
        }
        retPage.setList(goodsOrderVos);
        resultData.setBody(retPage);
        return resultData;
    }

    /**
     * 订单详情
     *
     * @param orderId 订单号
     * @return
     */
    @RequestMapping(value = "/detail")
    public ResultData detail(@RequestParam String orderId) {
        ResultData resultData = new ResultData();
        GoodsOrder goodsOrder = orderServiceFacade.selectGoodsOrderById(orderId);
        Map<String, String> expressMap = dictionaryManager.selectDictMapByType(Constants.DELIVERY_COMPANY_DICT_TYPE);
        String express = expressMap.get(goodsOrder.getExpressCode());
        goodsOrder.setExpressCode(express == null ? "未知发货服务" : express);
        GoodsOrderVo goodsOrderVo = baseMapper.map(goodsOrder,GoodsOrderVo.class);;
        if(StringUtils.isNotBlank(goodsOrder.getLogistics())) {
            JSONObject logJson = (JSONObject) JSONObject.parse(goodsOrder.getLogistics());
            goodsOrderVo.setLogdata(JSONObject.parseArray(logJson.getString("data"), LogisticsData.class));
        }
        List<GoodsOrderVo> goodsOrderVos = Lists.newArrayList(goodsOrderVo);
        dealGoodsOrder(goodsOrderVos);
        //ruanyj 查询商品图片，放到web层处理，商品图片和订单没有关系
        List<OrderDetailVo> orderDetails = goodsOrderVos.get(0).getOrderDetails();
        addGoodsPics(orderDetails);
        resultData.setBody(goodsOrderVos.get(0));
        return resultData;
    }
    /**
     * ruanyj 添加商品图片
     */
    private void addGoodsPics(List<OrderDetailVo> orderDetails) {
        if (CollectionUtils.isNotEmpty(orderDetails)) {
            List<Integer> goodsIds = Lists.newArrayList();
            for (OrderDetailVo orderDetail : orderDetails) {
                goodsIds.add(orderDetail.getGoodsId());
            }
            // goodsId -> goodsPics
            Map<Integer, List<GoodsPic>> picMap = selectGoodsPicByGoodsIds(goodsIds);
            for (OrderDetailVo orderDetail : orderDetails) {
                orderDetail.setGoodsPics(picMap.get(orderDetail.getGoodsId()));
            }
        }
    }

    /**
     * 确认订单
     *
     * @return
     */
    @RequestMapping(value = "/confirm")
    public ResultData confirm() {
        ResultData resultData = new ResultData();
        ConfirmOrderVo conOrderVo = orderManager.confirmOrder(UserHandleUtil.getUserId(), UserHandleUtil.getInsId());
        DecimalFormat df1 = new DecimalFormat("0.00");
        conOrderVo.setBalanceDis(df1.format(conOrderVo.getBalance()));
        conOrderVo.setGoodsAmountDis(df1.format(conOrderVo.getGoodsAmount()));
        for(ConfirmGoodsVo cgv:conOrderVo.getGoodsItem()) {
            cgv.setPriceDis(df1.format(cgv.getPrice()));
            cgv.setTotalDis(df1.format(cgv.getTotal()));
        }
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
    @RequestMapping(value = "/freight")
    public ResultData freight(@RequestParam Integer provinceId, Integer[] goodsTypeIds) {
        if(provinceId==null) {
            return ResultData.failed("收货人地址有误! ");
        }
        ResultData resultData = new ResultData();
        FreightVo freightVo =orderManager.reloadFreight(UserHandleUtil.getUserId(), UserHandleUtil.getInsId(),
                provinceId, goodsTypeIds == null ? null : Lists.newArrayList(goodsTypeIds));
        //ruanyj double展示保留两位小数
        DecimalFormat df1 = new DecimalFormat("0.00");
        freightVo.setBalanceDis(df1.format(freightVo.getBalance()));
        freightVo.setGoodsAmountDis(df1.format(freightVo.getGoodsAmount()));
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
    @RequestMapping(value = "/submit")
    public ResultData submit(@RequestParam Integer consigneeId, String receivePhone,
                             @RequestParam String express, Integer[] goodsTypeIds, @RequestParam String token) {
        ResultData resultData = new ResultData();
        try {
            validateInsType(); // 试用机构不能下单
            resultData.setBody(orderManager.submit(UserHandleUtil.getUserId(), UserHandleUtil.getInsId(), consigneeId,
                    receivePhone, express, goodsTypeIds == null ? null : Lists.newArrayList(goodsTypeIds), token));
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
     * 计算总件数/总金额等信息
     *
     * @param goodsOrderVos
     */
    private void dealGoodsOrder(List<GoodsOrderVo> goodsOrderVos) {
        if (CollectionUtils.isNotEmpty(goodsOrderVos)) {
            for (GoodsOrderVo goodsOrderVo : goodsOrderVos) {
                //ruanyj double展示保留两位小数
                DecimalFormat df1 = new DecimalFormat("0.00");
                // 订单总金额
                goodsOrderVo.setPayAmount(CalculateUtil.add(goodsOrderVo.getConsumeAmount(), goodsOrderVo.getFreight()));
                int goodsPieces = 0;
                List<Integer> goodsTypeIds = Lists.newArrayList();
                for (OrderDetailVo orderDetailVo : goodsOrderVo.getOrderDetails()) {
                    goodsPieces += orderDetailVo.getNum();
                    if (orderDetailVo.getGoodTypeId() != null) goodsTypeIds.add(orderDetailVo.getGoodTypeId());
                }
                Map<Integer, ConfirmGoodsVo> confirmGoodsVoMap = orderManager.findGoodsByTypeIds(goodsTypeIds);
                for (OrderDetailVo orderDetailVo : goodsOrderVo.getOrderDetails()) {
                    ConfirmGoodsVo confirmGoodsVo = confirmGoodsVoMap.get(orderDetailVo.getGoodTypeId());
                    orderDetailVo.setWeight(confirmGoodsVo == null ? 0 : confirmGoodsVo.getWeight());
                    orderDetailVo.setTotal(CalculateUtil.mul(orderDetailVo.getPrice(), orderDetailVo.getNum().doubleValue()));
                    //格式化double类型的数据
                    orderDetailVo.setPriceDis(orderDetailVo.getPrice()==null?"0.00":df1.format(orderDetailVo.getPrice()));
                    orderDetailVo.setTotalDis(orderDetailVo.getTotal()==null?"0.00":df1.format(orderDetailVo.getTotal()));
                }
                goodsOrderVo.setGoodsPieces(goodsPieces);
                //格式化double类型的数据
                goodsOrderVo.setFreightDis(goodsOrderVo.getFreight()==null?"0.00":df1.format(goodsOrderVo.getFreight()));
                goodsOrderVo.setConsumeAmountDis(goodsOrderVo.getConsumeAmount()==null?"0.00":df1.format(goodsOrderVo.getConsumeAmount()));
                goodsOrderVo.setPayAmountDis(goodsOrderVo.getPayAmount()==null?"0.00":df1.format(goodsOrderVo.getPayAmount()));
            }
        }
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
    /**
     * 订单所需的商品图片查询
     */
    private Map<Integer, List<GoodsPic>> selectGoodsPicByGoodsIds(List<Integer> goodsIds) {
        Map<Integer, List<GoodsPic>> picMap = Maps.newHashMap();
        List<GoodsPic> goodsPics = goodsPicServiceFacade.findGoodsPicByGoodsIds(goodsIds).getBody();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(goodsPics)) {
            for (GoodsPic goodsPic : goodsPics) {
                List<GoodsPic> goodsPicList = picMap.get(goodsPic.getGoodsId());
                if (org.apache.commons.collections4.CollectionUtils.isEmpty(goodsPicList)) {
                    goodsPicList = Lists.newArrayList();
                }
                goodsPicList.add(goodsPic);
                picMap.put(goodsPic.getGoodsId(), goodsPicList);
            }
        } // 商品图片
        return picMap;
    }
}

