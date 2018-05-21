package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.FinancialAccountManager;
import com.aixuexi.vampire.manager.ItemOrderManager;
import com.aixuexi.vampire.manager.OrderManager;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.basicdata.DictionaryApi;
import com.gaosi.api.basicdata.SubjectProductApi;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.UserService;
import com.gaosi.api.davincicode.model.User;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.dto.QueryOrderDto;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.ItemOrder;
import com.gaosi.api.revolver.util.ConstantsUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.bean.common.Assert;
import com.gaosi.api.vulcan.constant.GoodsTypePriceConstant;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallCategoryServiceFacade;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.model.MallCategory;
import com.gaosi.api.vulcan.model.MallItem;
import com.gaosi.api.vulcan.model.MallSku;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.*;
import com.gaosi.api.xmen.constant.DictConstants;
import com.gaosi.api.xmen.service.TalentDemandService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

import static com.aixuexi.vampire.util.Constants.ORDERDETAIL_NAME_DIV;

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
    private FinancialAccountService financialAccountService;

    @Resource
    private OrderServiceFacade orderServiceFacade;

    @Resource
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Resource
    private MallItemExtServiceFacade mallItemExtServiceFacade;

    @Resource
    private MallCategoryServiceFacade mallCategoryServiceFacade;

    @Resource
    private FinancialAccountManager financialAccountManager;

    @Resource
    private UserService userService;

    @Resource
    private InstitutionService institutionService;

    @Resource
    private DictionaryApi dictionaryApi;

    @Resource
    private SubjectProductApi subjectProductApi;

    @Resource
    private TalentDemandService talentDemandService;

    @Resource
    private BaseMapper baseMapper;


    /**
     * 根据条件查询我的订单列表
     *
     * @param queryOrderDto
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResultData list(QueryOrderDto queryOrderDto) {
        if (queryOrderDto == null) {
            return ResultData.failed("查询参数不能为空");
        }
        queryOrderDto.setInstitutionId(UserHandleUtil.getInsId());
        queryOrderDto.setUserId(UserHandleUtil.getUserId());
        if (queryOrderDto.getEndTime() != null) {
            Date endTime = queryOrderDto.getEndTime();
            endTime = new DateTime(endTime).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            queryOrderDto.setEndTime(endTime);
        }
        if (queryOrderDto.getCategoryId() == null || queryOrderDto.getCategoryId() == 0) {
            //默认加载教材订单列表
            queryOrderDto.setCategoryId(MallItemConstant.Category.JCZB.getId());
        }
        switch (MallItemConstant.Category.get(queryOrderDto.getCategoryId())){
            case LDPXSC:
            case DZFW:
            case RCZX:
                return queryItemOrder(queryOrderDto);
            case JCZB:
                return queryJCZB(queryOrderDto);
            default:
                return ResultData.failed("参数类型错误");
        }
    }

    /**
     * 查询教材周边订单列表
     *
     * @param queryOrderDto
     * @return
     */
    private ResultData queryJCZB(QueryOrderDto queryOrderDto) {
        ApiResponse<Page<GoodsOrderVo>> apiResponse = orderServiceFacade.queryGoodsOrder(queryOrderDto);
        ApiResponseCheck.check(apiResponse);
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
     * 查询虚拟商品订单列表
     *
     * @param queryOrderDto
     * @return
     */
    private ResultData queryItemOrder(QueryOrderDto queryOrderDto) {
        ApiResponse<Page<ItemOrderVo>> apiResponse = itemOrderServiceFacade.queryItemOrder(queryOrderDto);
        ApiResponseCheck.check(apiResponse);
        //查询成功
        Page<ItemOrderVo> page = apiResponse.getBody();
        //总数为0就不进行其他操作了
        if (page.getItemTotal() == 0) {
            return ResultData.successed(page);
        }
        List<ItemOrderVo> itemOrderVos = page.getList();
        List<Integer> categoryIds = new ArrayList<>(CollectionCommonUtil.getFieldSetByObjectList(itemOrderVos,"getCategoryId",Integer.class));
        ApiResponse<List<MallCategory>> mallCategoryByIds = mallCategoryServiceFacade.findMallCategoryByIds(categoryIds);
        ApiResponseCheck.check(mallCategoryByIds);
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
    public ResultData nailSubmit(@RequestParam Integer itemId, @RequestParam Integer itemCount) {
        logger.info("userId=[{}] submit order, itemId=[{}], itemCount=[{}].", UserHandleUtil.getUserId(), itemId, itemCount);
        //根据商品id查询商品
        ApiResponse<MallItemNailVo> mallItemNailVoResponse = mallItemExtServiceFacade.queryMallItemNailDetail(itemId,MallItemConstant.ShelvesStatus.ON);
        ApiResponseCheck.check(mallItemNailVoResponse);
        MallItemNailVo mallItemNailVo = mallItemNailVoResponse.getBody();
        Assert.notNull(mallItemNailVo,"未查询到该校长培训");
        Assert.isTrue(mallItemNailVo.getSignUpStatus() != MallItemConstant.SignUpStatus.NO_START,"报名未开始");
        Assert.isTrue(mallItemNailVo.getSignUpStatus() != MallItemConstant.SignUpStatus.FINISHED,"报名已结束");
        //仅在限制报名人数时做判断
        if (mallItemNailVo.getSignUpNum() > 0) {
            //已报名数量查询
            ApiResponse<List<ItemOrderStatisVo>> itemOrderStatisVoResponse = itemOrderServiceFacade.getCountByItemId(Lists.newArrayList(itemId));
            ApiResponseCheck.check(itemOrderStatisVoResponse);
            List<ItemOrderStatisVo> itemOrderStatisVos = itemOrderStatisVoResponse.getBody();
            Map<Integer, ItemOrderStatisVo> map = CollectionCommonUtil.toMapByList(itemOrderStatisVos, "getItemId", Integer.class);
            int signedUpNum = map.get(itemId).getSignedTotal();
            //计算剩余数量
            int remain = mallItemNailVo.getSignUpNum() - signedUpNum;
            if (remain < itemCount) {
                int overCount = itemCount - remain;
                return ResultData.failed("超过报名数量:" + overCount);
            }
        }
        MallItem mallItem = new MallItem();
        mallItem.setId(mallItemNailVo.getMallItemId());
        mallItem.setName(mallItemNailVo.getName());
        mallItem.setCategoryId(mallItemNailVo.getCategoryId());
        // 校长培训只有一个SKU
        MallSku mallSku = mallItemNailVo.getMallSkuList().get(0);
        ItemOrderVo itemOrderVo = itemOrderManager.generateItemOrderVo(mallItem, mallSku, itemCount);
        String orderId = itemOrderManager.submit(itemOrderVo);
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
        logger.info("userId=[{}] customServiceSubmit, itemId=[{}], itemCount=[{}].", UserHandleUtil.getUserId(), itemId, itemCount);
        ApiResponse<ConfirmCustomServiceVo> apiResponse = mallItemExtServiceFacade.confirmMallItem4DZFW(itemId,itemCount);
        ApiResponseCheck.check(apiResponse);
        ConfirmCustomServiceVo confirmCustomServiceVo = apiResponse.getBody();
        MallItem mallItem = baseMapper.map(confirmCustomServiceVo,MallItem.class);
        MallSku mallSku = new MallSku();
        mallSku.setPrice(confirmCustomServiceVo.getPrice());
        ItemOrderVo itemOrderVo = itemOrderManager.generateItemOrderVo(mallItem, mallSku, itemCount);
        String orderId = itemOrderManager.submit(itemOrderVo);
        return ResultData.successed(orderId);
    }

    /**
     * 人才中心订单提交
     * @param talentOrderVo
     * @return
     */
    @RequestMapping(value = "/talentCenter/submit", method = RequestMethod.POST)
    public ResultData talentCenterSubmit(@RequestBody TalentOrderVo talentOrderVo) {
        logger.info("userId=[{}] talentCenterSubmit, talentOrderVo=[{}]", UserHandleUtil.getUserId(), talentOrderVo);
        ReqTalentCenterConditionVo reqTalentCenterConditionVo = new ReqTalentCenterConditionVo();
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setMallItemId(talentOrderVo.getMallItemId());
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        ApiResponse<ConfirmTalentVo> confirmTalentVoResponse = mallItemExtServiceFacade.confirmTalentCenter(reqTalentCenterConditionVo, talentOrderVo.getMallSkuId(), talentOrderVo.getNum());
        ConfirmTalentVo confirmTalentVo = confirmTalentVoResponse.getBody();
        MallItem mallItem = new MallItem();
        mallItem.setId(talentOrderVo.getMallItemId());
        mallItem.setName(confirmTalentVo.getName());
        mallItem.setCategoryId(MallItemConstant.Category.RCZX.getId());
        MallSku mallSku = new MallSku();
        mallSku.setId(confirmTalentVo.getMallSkuId());
        mallSku.setPrice(confirmTalentVo.getPrice());
        mallSku.setName(confirmTalentVo.getEducationRemark() + ORDERDETAIL_NAME_DIV + confirmTalentVo.getExperienceRemark());
        ItemOrderVo itemOrderVo = itemOrderManager.generateItemOrderVo(mallItem, mallSku, talentOrderVo.getNum());
        List<TalentTemplateVo> talentTemplateVos = talentOrderVo.getTalentTemplateVos();
        String extInfo = JSONObject.toJSONString(talentTemplateVos);
        itemOrderVo.setExtInfo(extInfo);
        String orderId = itemOrderManager.submit(itemOrderVo);
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
        logger.info("userId=[{}] pay order, orderId=[{}], token=[{}].", UserHandleUtil.getUserId(), orderId, token);
        if (StringUtils.isBlank(orderId)) {
            return ResultData.failed("订单号为空");
        }
        if (StringUtils.isBlank(token)) {
            return ResultData.failed("token为空");
        }
        ItemOrder itemOrder = itemOrderManager.getOrderByOrderId(orderId);
        if (itemOrder.getStatus() == OrderConstant.OrderStatus.CANCELLED.getValue()) {
            // 防止用户在确认支付页面停留时间超过规定支付时间，订单已取消仍可支付的情况出现
            return ResultData.failed("支付超时，该订单已自动取消");
        }
        financialAccountManager.pay(orderId, token, itemOrder.getCategoryId(), itemOrder.getConsumeCount());
        itemOrderManager.updateOrderStatus(orderId);
        return ResultData.successed(orderId);
    }

    @RequestMapping(value = "/lockPay", method = RequestMethod.POST)
    public ResultData lockPay(@RequestParam String orderId, @RequestParam String token) {
        Integer userId = UserHandleUtil.getUserId();
        Integer insId = UserHandleUtil.getInsId();
        logger.info("userId=[{}] lockPay, orderId=[{}], token=[{}].", userId, orderId, token);
        // 订单支付
        ApiResponse<ItemOrderVo> itemOrderResponse = itemOrderServiceFacade.payItemOrder(orderId, token, insId, userId);
        ApiResponseCheck.check(itemOrderResponse);
        ItemOrderVo itemOrderVo = itemOrderResponse.getBody();
        // 人才中心工单
        TalentWorkOrderVo talentWorkOrderVo = generateTalentWorkOrderVo(itemOrderVo,userId,insId);
        List<String> workOrderIds = talentDemandService.saveTicketsRetWorkOrderList(talentWorkOrderVo);
        // 更新关联工单号
        ItemOrder itemOrder = new ItemOrder();
        itemOrder.setOrderId(orderId);
        // 工单号分隔符
        String SEPARATOR = ",";
        StringBuilder workOrderIdBuilder = new StringBuilder();
        for (String workOrderId : workOrderIds) {
            workOrderIdBuilder.append(workOrderId);
            workOrderIdBuilder.append(SEPARATOR);
        }
        itemOrder.setRelationInfo(workOrderIdBuilder.toString());
        itemOrderServiceFacade.updateOrder(itemOrder);
        return ResultData.successed(orderId);
    }

    /**
     * 生成人才中心工单类型对象
     * @param itemOrderVo
     * @param userId
     * @param insId
     * @return
     */
    private TalentWorkOrderVo generateTalentWorkOrderVo(ItemOrderVo itemOrderVo, Integer userId,Integer insId) {
        TalentWorkOrderVo talentWorkOrderVo = new TalentWorkOrderVo();
        // 用户信息
        talentWorkOrderVo.setUserId(userId);
        User user = userService.getUserById(userId);
        talentWorkOrderVo.setUserName(user.getName());
        talentWorkOrderVo.setTelephone(user.getTelephone());
        // 机构信息
        talentWorkOrderVo.setInsititutionId(insId);
        Institution institution = institutionService.getInsInfoById(insId);
        talentWorkOrderVo.setInsititutionName(institution.getName());
        talentWorkOrderVo.setInsititutionAddress(institution.getAddress());
        // 订单信息
        talentWorkOrderVo.setOrderId(itemOrderVo.getOrderId());
        talentWorkOrderVo.setCreateTime(itemOrderVo.getCreateTime());
        String extInfo = itemOrderVo.getExtInfo();
        List<TalentTemplateVo> talentTemplateVos = (List<TalentTemplateVo>)JSONObject.parse(extInfo);
        talentWorkOrderVo.setTalentTemplateVos(talentTemplateVos);
        // 订单详情
        List<ItemOrderDetailVo> itemOrderDetails = itemOrderVo.getItemOrderDetails();
        ItemOrderDetailVo itemOrderDetailVo = itemOrderDetails.get(0);
        talentWorkOrderVo.setNum(itemOrderDetailVo.getItemCount());
        // 订单的商品信息
        ReqTalentCenterConditionVo reqTalentCenterConditionVo = new ReqTalentCenterConditionVo();
        reqTalentCenterConditionVo.setMallItemId(itemOrderDetailVo.getItemId());
        ApiResponse<MallItemTalentVo> mallItemTalentResponse = mallItemExtServiceFacade.queryMallItem4Talent(reqTalentCenterConditionVo);
        MallItemTalentVo mallItemTalentVo = mallItemTalentResponse.getBody();
        // 人才类型
        talentWorkOrderVo.setTypeCode(mallItemTalentVo.getTypeCode());
        ApiResponse<DictionaryBo> dictionaryResponse = dictionaryApi.getByTypeAndCode(DictConstants.TALENT_TYPE, mallItemTalentVo.getTypeCode());
        ApiResponseCheck.check(dictionaryResponse);
        DictionaryBo dictionaryBo = dictionaryResponse.getBody();
        talentWorkOrderVo.setType(dictionaryBo.getName());
        // 学科
        talentWorkOrderVo.setSubjectProductId(mallItemTalentVo.getSubjectProductId());
        ApiResponse<SubjectProductBo> subjectResponse = subjectProductApi.getById(mallItemTalentVo.getSubjectProductId());
        ApiResponseCheck.check(subjectResponse);
        SubjectProductBo subjectProductBo = subjectResponse.getBody();
        talentWorkOrderVo.setSubjectProduct(subjectProductBo.getName());
        // 学历，经验
        List<MallSkuExtTalentVo> mallSkuExtTalentVos = mallItemTalentVo.getMallSkuExtTalentVos();
        for (MallSkuExtTalentVo mallSkuExtTalentVo : mallSkuExtTalentVos) {
            if(mallSkuExtTalentVo.getMallSkuId().equals(itemOrderDetailVo.getMallSkuId())){
                talentWorkOrderVo.setEducationCode(mallSkuExtTalentVo.getEducationCode());
                talentWorkOrderVo.setEducationRemark(mallSkuExtTalentVo.getEducationRemark());
                talentWorkOrderVo.setExperienceCode(mallSkuExtTalentVo.getExperienceCode());
                talentWorkOrderVo.setExperienceRemark(mallSkuExtTalentVo.getExperienceRemark());
            }
        }
        return talentWorkOrderVo;
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
        String token = financialAccountService.getTokenForFinancial();
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
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        ItemOrder itemOrder = itemOrderManager.getOrderByOrderId(orderId);
        AmountVo amountVo = new AmountVo();
        Double remainAmount = rr.getUsableRemain().doubleValue() / 10000;
        amountVo.setRemainAmount(remainAmount);
        amountVo.setConsumeCount(itemOrder.getConsumeCount());
        return ResultData.successed(amountVo);
    }

    /**
     * 订单状态统计
     * @return
     */
    @RequestMapping(value = "/getStatusTotal", method = RequestMethod.GET)
    public ResultData getStatusTotal() {
        ApiResponse<List<OrderStatusTotalVo>> apiResponse = orderServiceFacade.queryOrderStatusTotal(UserHandleUtil.getInsId());
        ApiResponseCheck.check(apiResponse);
        return ResultData.successed(apiResponse.getBody());
    }
}
