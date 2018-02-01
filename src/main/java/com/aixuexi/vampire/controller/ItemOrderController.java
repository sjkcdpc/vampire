package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.ItemOrderManager;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.revolver.dto.QueryItemOrderDto;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.ItemOrder;
import com.gaosi.api.revolver.util.ConstantsUtil;
import com.gaosi.api.revolver.vo.AmountVo;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.revolver.vo.ItemOrderStatisVo;
import com.gaosi.api.revolver.vo.ItemOrderVo;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallCategoryServiceFacade;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.model.MallCategory;
import com.gaosi.api.vulcan.model.MallItem;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.CategoryVo;
import com.gaosi.api.vulcan.vo.ConfirmCustomServiceVo;
import com.gaosi.api.vulcan.vo.MallItemNailVo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Resource
    private FinancialAccountService finAccService;

    @Resource
    private OrderServiceFacade orderServiceFacade;

    @Resource
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Resource
    private MallItemExtServiceFacade mallItemExtServiceFacade;

    @Resource
    private MallCategoryServiceFacade mallCategoryServiceFacade;

    @Resource
    private BaseMapper baseMapper;


    /**
     * 根据条件查询我的订单列表
     *
     * @param queryItemOrderDto
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResultData list(QueryItemOrderDto queryItemOrderDto) {
        if (queryItemOrderDto == null) {
            return ResultData.failed("查询参数不能为空");
        }
        queryItemOrderDto.setInstitutionId(UserHandleUtil.getInsId());
        queryItemOrderDto.setUserId(UserHandleUtil.getUserId());
        if (queryItemOrderDto.getEndTime() != null) {
            Date endTime = queryItemOrderDto.getEndTime();
            endTime = new DateTime(endTime).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            queryItemOrderDto.setEndTime(endTime);
        }
        if (queryItemOrderDto.getCategoryId() == null || queryItemOrderDto.getCategoryId() == 0) {
            //默认加载教材订单列表
            queryItemOrderDto.setCategoryId(MallItemConstant.Category.JCZB.getId());
        }
        if (queryItemOrderDto.getCategoryId().equals(MallItemConstant.Category.LDPXSC.getId())||queryItemOrderDto.getCategoryId().equals(MallItemConstant.Category.DZFW.getId())) {
            return queryLDPXSC(queryItemOrderDto);
        }
        if (queryItemOrderDto.getCategoryId().equals(MallItemConstant.Category.JCZB.getId())) {
            return queryJCZB(queryItemOrderDto);
        }
        return ResultData.failed("参数类型错误");
    }

    /**
     * 查询教材周边订单列表
     *
     * @param queryItemOrderDto
     * @return
     */
    private ResultData queryJCZB(QueryItemOrderDto queryItemOrderDto) {
        ApiResponse<Page<GoodsOrderVo>> apiResponse = orderServiceFacade.queryGoodsOrder(queryItemOrderDto);
        //响应错误直接返回
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(apiResponse.getMessage());
        }
        //查询成功
        Page<GoodsOrderVo> page = apiResponse.getBody();
        //总数为0就不进行其他操作了
        if (page.getItemTotal() == 0) {
            return ResultData.successed(page);
        }
        List<GoodsOrderVo> goodsOrderVos = page.getList();
        // 列表不需要图片等详情
        orderManager.dealGoodsOrderVos(goodsOrderVos, false);
        return ResultData.successed(page);
    }

    /**
     * 查询校长培训订单列表
     *
     * @param queryItemOrderDto
     * @return
     */
    private ResultData queryLDPXSC(QueryItemOrderDto queryItemOrderDto) {
        ApiResponse<Page<ItemOrderVo>> apiResponse = itemOrderServiceFacade.queryItemOrder(queryItemOrderDto);
        //响应错误直接返回
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(apiResponse.getMessage());
        }
        //查询成功
        Page<ItemOrderVo> page = apiResponse.getBody();
        //总数为0就不进行其他操作了
        if (page.getItemTotal() == 0) {
            return ResultData.successed(page);
        }
        List<ItemOrderVo> itemOrderVos = page.getList();
        List<Integer> categoryIds = new ArrayList<>(CollectionCommonUtil.getFieldSetByObjectList(itemOrderVos,"getCategoryId",Integer.class));
        ApiResponse<List<MallCategory>> mallCategoryByIds = mallCategoryServiceFacade.findMallCategoryByIds(categoryIds);
        //响应错误直接返回
        if (mallCategoryByIds.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(mallCategoryByIds.getMessage());
        }
        List<MallCategory> mallCategories = mallCategoryByIds.getBody();
        Map<Integer, MallCategory> map = CollectionCommonUtil.toMapByList(mallCategories, "getId", Integer.class);
        for (ItemOrderVo itemOrderVo : itemOrderVos) {
            String categoryName = map.get(itemOrderVo.getCategoryId()).getName();
            itemOrderVo.setCategoryName(categoryName);
        }
        page.setList(itemOrderVos);
        return ResultData.successed(page);
    }


    /**
     * 钉子提交订单
     *
     * @param itemId
     * @param itemCount
     * @return
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public ResultData submit(@RequestParam Integer itemId, @RequestParam Integer itemCount) {
        logger.info("userId=[{}] submit order, itemId=[{}], itemCount=[{}].", UserSessionHandler.getId(), itemId, itemCount);
        if (itemId == null || itemCount == null) {
            return ResultData.failed("参数错误");
        }
        //根据商品id查询商品
        ApiResponse<MallItemNailVo> voApiResponse = mallItemExtServiceFacade.queryMallItemNailDetail(itemId,MallItemConstant.ShelvesStatus.ON);
        if (voApiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(voApiResponse.getMessage());
        }
        if (voApiResponse.getBody() == null) {
            return ResultData.failed("未查询到该校长培训");
        }
        MallItemNailVo mallItemNailVo = voApiResponse.getBody();
        if (mallItemNailVo.getSignUpStatus() == MallItemConstant.SignUpStatus.NO_START) {
            return ResultData.failed("报名未开始");
        }
        if (mallItemNailVo.getSignUpStatus() == MallItemConstant.SignUpStatus.FINISHED) {
            return ResultData.failed("报名已结束");
        }
        //已报名数量查询
        ApiResponse<List<ItemOrderStatisVo>> apiResponse = itemOrderServiceFacade.getCountByItemId(Lists.newArrayList(itemId));
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed("查询已报名数量失败");
        }
        //仅在限制报名人数时做判断
        if (mallItemNailVo.getSignUpNum() > 0) {
            List<ItemOrderStatisVo> itemOrderStatisVos = apiResponse.getBody();
            Map<Integer, ItemOrderStatisVo> map = CollectionCommonUtil.toMapByList(itemOrderStatisVos, "getItemId", Integer.class);
            int signedUpNum = map.get(itemId).getSignedTotal();
            //计算剩余数量
            int remain = mallItemNailVo.getSignUpNum() - signedUpNum;
            if (remain < itemCount) {
                int overCount = itemCount - remain;
                return ResultData.failed("超过报名数量:" + overCount);
            }
        }
        MallItem mallItem = baseMapper.map(mallItemNailVo,MallItem.class);
        String orderId = itemOrderManager.submit(mallItem, itemCount, UserHandleUtil.getInsId(),mallItemNailVo.getPrice(),mallItemNailVo.getOriginalPrice());
        return ResultData.successed(orderId);
    }

    /**
     * 定制服务提交订单
     *
     * @param itemId
     * @param itemCount
     * @return
     */
    @RequestMapping(value = "/customService/submit", method = RequestMethod.POST)
    public ResultData customServiceSubmit(@RequestParam Integer itemId, @RequestParam Integer itemCount) {
        logger.info("userId=[{}] customServiceSubmit, itemId=[{}], itemCount=[{}].", UserSessionHandler.getId(), itemId, itemCount);
        if (itemId == null || itemCount == null) {
            return ResultData.failed("参数错误");
        }
        ApiResponse<ConfirmCustomServiceVo> apiResponse = mallItemExtServiceFacade.confirmMallItem4DZFW(itemId,itemCount);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed(apiResponse.getMessage());
        }
        ConfirmCustomServiceVo confirmCustomServiceVo = apiResponse.getBody();
        MallItem mallItem = baseMapper.map(confirmCustomServiceVo,MallItem.class);
        String orderId = itemOrderManager.submit(mallItem, itemCount, UserHandleUtil.getInsId(),confirmCustomServiceVo.getPrice(),confirmCustomServiceVo.getPrice());
        return ResultData.successed(orderId);
    }

    /**
     * 订单支付
     *
     * @param orderId
     * @return
     */
    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public ResultData pay(@RequestParam String orderId, @RequestParam String token) {
        logger.info("userId=[{}] pay order, orderId=[{}], token=[{}].", UserSessionHandler.getId(), orderId, token);
        if (StringUtils.isBlank(orderId)) {
            return ResultData.failed("订单号为空");
        }
        if (StringUtils.isBlank(token)) {
            return ResultData.failed("token为空");
        }
        itemOrderManager.pay(orderId, token);
        return ResultData.successed(orderId);
    }

    /**
     * 获取类型和订单状态
     *
     * @return
     */
    @RequestMapping(value = "/getTypeStatus", method = RequestMethod.GET)
    public ResultData getTypeStatus() {
        Map<String, Object> map = new HashMap<>();
        ApiResponse<List<MallCategory>> apiResponse = mallCategoryServiceFacade.findMallCategoryByLevel(1);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
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
     *
     * @return
     */
    @RequestMapping(value = "/getToken", method = RequestMethod.GET)
    public ResultData getToken() {
        String token = finAccService.getTokenForFinancial();
        return ResultData.successed(token);
    }

    /**
     * 我的订单列表点支付需要
     *
     * @return
     */
    @RequestMapping(value = "/getAmount", method = RequestMethod.GET)
    public ResultData getAmount(@RequestParam String orderId) {
        //查询当前机构账号余额
        RemainResult rr = finAccService.getRemainByInsId(UserHandleUtil.getInsId());
        if (rr == null) {
            return ResultData.failed("账户不存在");
        }
        ItemOrder itemOrder = itemOrderManager.getOrderByOrderId(orderId);
        AmountVo amountVo = new AmountVo();
        Double remainAmount = rr.getUsableRemain().doubleValue() / 10000;
        amountVo.setRemainAmount(remainAmount);
        amountVo.setConsumeCount(itemOrder.getConsumeCount());
        return ResultData.successed(amountVo);
    }
}
