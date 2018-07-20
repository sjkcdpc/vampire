package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.BasicDataManager;
import com.aixuexi.vampire.manager.WorkOrderManager;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.dragonball.constants.ApprovalConstant;
import com.gaosi.api.dragonball.model.co.WorkFlowApplyCondition;
import com.gaosi.api.dragonball.service.WorkFlowApplyService;
import com.gaosi.api.revolver.constant.ExpressConstant;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.constant.WorkOrderConstant;
import com.gaosi.api.revolver.dto.QueryWorkOrderRefundDto;
import com.gaosi.api.revolver.facade.ExpressServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.facade.SubOrderServiceFacade;
import com.gaosi.api.revolver.facade.WorkOrderRefundFacade;
import com.gaosi.api.revolver.model.Express;
import com.gaosi.api.revolver.util.JsonUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.vulcan.constant.GoodsExtConstant;
import com.gaosi.api.vulcan.facade.GoodsExtServiceFacade;
import com.gaosi.api.vulcan.facade.GoodsTypeServiceFacade;
import com.gaosi.api.vulcan.model.GoodsExt;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by ruanyanjie on 2018/7/6.
 */
@RestController
@RequestMapping("/workOrder")
public class WorkOrderController {
    private static Logger logger = LoggerFactory.getLogger(WorkOrderController.class);

    @Resource
    private WorkOrderRefundFacade workOrderRefundFacade;
    @Resource
    private OrderServiceFacade orderServiceFacade;
    @Resource
    private GoodsTypeServiceFacade goodsTypeServiceFacade;
    @Resource
    private WorkOrderManager workOrderManager;
    @Resource
    private ExpressServiceFacade expressServiceFacade;
    @Resource
    private WorkFlowApplyService workFlowApplyService;
    @Resource
    private BasicDataManager basicDataManager;
    @Resource
    private GoodsExtServiceFacade goodsExtServiceFacade;
    @Resource
    private SubOrderServiceFacade subOrderServiceFacade;

    /**
     * 售后工单列表查询
     * @return
     */
    @RequestMapping(value = "/refund", method = RequestMethod.GET)
    public ResultData queryRefundList(QueryWorkOrderRefundDto queryWorkOrderRefundDto){
        if (queryWorkOrderRefundDto == null){
            return ResultData.failed("查询条件不能为空");
        }
        // 查询当前机构下发起的工单
        queryWorkOrderRefundDto.setInstitutionId(UserHandleUtil.getInsId());
        ApiResponse<Page<WorkOrderRefundVo>> pageResponse = workOrderRefundFacade.queryRefundVoList(queryWorkOrderRefundDto);
        Page<WorkOrderRefundVo> page = pageResponse.getBody();
        //总数为0就不进行其他操作了
        if (page.getItemTotal() == 0){
            return ResultData.successed(page);
        }
        List<WorkOrderRefundVo> workOrderRefundVos = page.getList();
        List<WorkOrderRefundDetailVo> workOrderRefundDetailVos = new ArrayList<>();
        for (WorkOrderRefundVo workOrderRefundVo : workOrderRefundVos) {
            workOrderRefundDetailVos.addAll(workOrderRefundVo.getWorkOrderRefundDetailVos());
        }
        workOrderManager.dealWorkOrderRefundDetailVo(workOrderRefundDetailVos);
        return ResultData.successed(page);
    }

    /**
     * 申请售后
     * @return
     */
    @RequestMapping(value = "/afterSales", method = RequestMethod.GET)
    public ResultData afterSales(@RequestParam String oldOrderId,@RequestParam Integer mallSkuId){
        if (isDiy(mallSkuId)){
            return ResultData.failed("该商品不能申请售后");
        }
        ApiResponse<WorkOrderRefundDetailVo> afterSalesResponse = workOrderRefundFacade.applyAfterSales(oldOrderId, mallSkuId);
        WorkOrderRefundDetailVo workOrderRefundDetailVo = afterSalesResponse.getBody();
        workOrderManager.dealWorkOrderRefundDetailVo(Lists.newArrayList(workOrderRefundDetailVo));
        Map<String, Object> map = new HashMap<>();
        // 工单详情
        map.put("workOrderRefundDetailVo", workOrderRefundDetailVo);
        // 售后类型
        map.put("afterSalesTypes",WorkOrderConstant.AfterSalesType.getAll());
        // 退款原因
        map.put("refundReasons",WorkOrderConstant.RefundReason.getAll());
        return ResultData.successed(map);
    }

    /**
     * 提交退款申请
     * @param workOrderRefundVo
     * @return
     */
    @RequestMapping(value = "/refund/add", method = RequestMethod.POST)
    public ResultData addRefund(@RequestBody WorkOrderRefundVo workOrderRefundVo){
        List<WorkOrderRefundDetailVo> workOrderRefundDetailVos = workOrderRefundVo.getWorkOrderRefundDetailVos();
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundDetailVos) {
            if (workOrderRefundDetailVo.getTotalNum() <= 0) {
                return ResultData.failed("售后数量错误");
            }
            if (isDiy(workOrderRefundDetailVo.getMallSkuId())){
                return ResultData.failed("包含不能申请售后商品，请查看");
            }
        }
        // 原始订单号
        String oldOrderId = workOrderRefundVo.getOldOrderId();
        // 父订单号
        String parentOrderId = oldOrderId;
        if (oldOrderId.contains(OrderConstant.SUB_ORDER_ID_FLAG)) {
            parentOrderId = oldOrderId.substring(0,oldOrderId.indexOf(OrderConstant.SUB_ORDER_ID_FLAG));
        }
        // 查询原始订单信息
        ApiResponse<GoodsOrderVo> goodsOrderVoResponse = orderServiceFacade.getGoodsOrderById(parentOrderId);
        GoodsOrderVo goodsOrderVo = goodsOrderVoResponse.getBody();
        // 订单状态
        Integer status;
        // 订单完成时间
        Date updateTime;
        if (oldOrderId.contains(OrderConstant.SUB_ORDER_ID_FLAG)) {
            ApiResponse<SubGoodsOrderVo> subGoodsOrderResponse = subOrderServiceFacade.getSubGoodsOrderById(oldOrderId);
            SubGoodsOrderVo subGoodsOrderVo = subGoodsOrderResponse.getBody();
            status = subGoodsOrderVo.getStatus();
            updateTime = subGoodsOrderVo.getUpdateTime();
        }else {
            status = goodsOrderVo.getStatus();
            updateTime = goodsOrderVo.getUpdateTime();
        }
        //审批类型
        workOrderRefundVo.setApproveType(WorkOrderConstant.ApproveType.AUTO_APPROVE);
        if (status.equals(OrderConstant.OrderStatus.COMPLETED.getValue())) {
            Interval interval = new Interval(updateTime.getTime(), System.currentTimeMillis());
            long between = interval.toDurationMillis();
            if (between > WorkOrderConstant.APPROVE_TIMESPAN){
                workOrderRefundVo.setApproveType(WorkOrderConstant.ApproveType.ARTIFICIAL_APPROVE);
            }
        }
        workOrderRefundVo.setAreaId(goodsOrderVo.getAreaId());
        // 地址信息
        Integer areaId = workOrderRefundVo.getAreaId();
        Map<Integer, AddressDTO> addressMap = basicDataManager.getAddressByAreaIds(Lists.newArrayList(areaId));
        AddressDTO address = addressMap.get(areaId);
        workOrderRefundVo.setProvinceId(address.getProvinceId());
        // 创建退货审批流
        Integer approveId = createWorkFlow(workOrderRefundVo.getApproveType(), workOrderRefundVo.getType());
        workOrderRefundVo.setApproveId(approveId);
        workOrderRefundVo.setCreatorId(UserSessionHandler.getId());
        // 查询商品规格详情
        Map<Integer, GoodsType> goodsTypeMap = queryGoodsTypes(workOrderRefundDetailVos);
        // 填充重量
        workOrderManager.dealWorkOrderRefundVo(workOrderRefundVo,goodsTypeMap);
        // 创建退款工单
        ApiResponse<String> apiResponse = workOrderRefundFacade.create(workOrderRefundVo);
        String workOrderCode = apiResponse.getBody();
        return ResultData.successed(workOrderCode);
    }

    /**
     * 创建审批流
     * @param approveType 审批类型：1自动审批，2需要人工审批
     * @param refundType 退款类型：1仅退款，2退货退款
     * @return
     */
    private Integer createWorkFlow(Integer approveType, Integer refundType){
        WorkFlowApplyCondition workFlowApplyCondition = new WorkFlowApplyCondition();
        workFlowApplyCondition.setOrigin(ApprovalConstant.ORIGIN_SHOPPING_MANAGEMENT);
        workFlowApplyCondition.setType(ApprovalConstant.ORDER_TYPE_REFUND_MANAGEMENT);
        workFlowApplyCondition.setUserId(UserSessionHandler.getId());
        StringBuilder modelJson = new StringBuilder();
        if (approveType.equals(WorkOrderConstant.ApproveType.AUTO_APPROVE)){
            modelJson.append("{'approveType':").append(approveType).append(",'refundType':").append(refundType).append("}");
        }else {
            modelJson.append("{'approveType':").append(approveType).append("}");
        }
        workFlowApplyCondition.setModelJson(modelJson.toString());
        ApiResponse<Integer> workFlowResponse = workFlowApplyService.save(workFlowApplyCondition);
        return workFlowResponse.getBody();
    }

    /**
     * 判断某个商品是不是DIY
     * @param mallSkuId
     * @return
     */
    private boolean isDiy(Integer mallSkuId){
        ApiResponse<GoodsExt> apiResponse = goodsExtServiceFacade.queryByMallSkuId(mallSkuId);
        GoodsExt goodsExt = apiResponse.getBody();
        return goodsExt.getCustomized().equals(GoodsExtConstant.Customized.DIY_CUSTOM.getValue());
    }

    /**
     * 查询商品规格
     * @param workOrderRefundDetailVos
     * @return
     */
    private Map<Integer, GoodsType> queryGoodsTypes(List<WorkOrderRefundDetailVo> workOrderRefundDetailVos){
        Set<Integer> mallSkuIds = CollectionCommonUtil.getFieldSetByObjectList(workOrderRefundDetailVos,
                "getMallSkuId", Integer.class);
        ApiResponse<List<GoodsType>> goodsTypeResponse = goodsTypeServiceFacade.queryByMallSkuIds(Lists.newArrayList(mallSkuIds));
        return CollectionCommonUtil.toMapByList(goodsTypeResponse.getBody(), "getMallSkuId", Integer.class);
    }

    /**
     * 查看退款工单
     * @param workOrderCode
     * @return
     */
    @RequestMapping(value = "/refund/detail", method = RequestMethod.GET)
    public ResultData queryRefundDetail(@RequestParam String workOrderCode){
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderCode);
        WorkOrderRefundVo workOrderRefundVo = refundVoResponse.getBody();
        workOrderManager.dealWorkOrderRefundVo(workOrderRefundVo);
        Map<String, Object> map = new HashMap<>();
        map.put("workOrderRefundVo", workOrderRefundVo);
        Integer status = workOrderRefundVo.getStatus();
        // 未审批，修改工单
        if (status == WorkOrderConstant.RefundStatus.NO_APPROVE) {
            // 售后类型
            map.put("afterSalesTypes",WorkOrderConstant.AfterSalesType.getAll());
            // 退款原因
            map.put("refundReasons",WorkOrderConstant.RefundReason.getAll());
        }
        // 审批通过，补充物流信息
        if (status == WorkOrderConstant.RefundStatus.ONE_SUCCESS || status == WorkOrderConstant.RefundStatus.RETURN_GOODS) {
            ApiResponse<List<Express>> expressResponse = expressServiceFacade.queryAllExpress();
            List<Express> expressList = expressResponse.getBody();
            map.put("express", expressList);
            if(workOrderRefundVo.getExpressCode().equals(ExpressConstant.OTHER_EXPRESS_CODE)){
                Express express = new Express();
                express.setCode(workOrderRefundVo.getExpressCode());
                express.setName(workOrderRefundVo.getExpressName());
            }
        }
        return ResultData.successed(map);
    }

    /**
     * 查看退款工单协商历史记录
     * @param workOrderCode
     * @return
     */
    @RequestMapping(value = "/refund/detail/record", method = RequestMethod.GET)
    public ResultData queryRefundRecord(@RequestParam String workOrderCode, Integer dealRecordId, Integer pageSize){
        ApiResponse<List<WorkOrderDealRecordVo>> apiResponse = workOrderRefundFacade.queryRecordPage(workOrderCode, dealRecordId, pageSize);
        List<WorkOrderDealRecordVo> workOrderDealRecordVos = apiResponse.getBody();
        workOrderManager.dealRefundDealRecords(workOrderDealRecordVos);
        return ResultData.successed(workOrderDealRecordVos);
    }

    /**
     * 修改退货工单
     * @param workOrderRefundVo
     * @return
     */
    @RequestMapping(value = "/refund/update", method = RequestMethod.POST)
    public ResultData updateRefund(@RequestBody WorkOrderRefundVo workOrderRefundVo){
        Integer userId = UserSessionHandler.getId();
        logger.info("update refund - userId:{}, workOrderRefund:{}", userId, workOrderRefundVo);
        // 工单号
        String workOrderCode = workOrderRefundVo.getWorkOrderCode();
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderCode);
        WorkOrderRefundVo result = refundVoResponse.getBody();
        // 工单处于未审批
        if (WorkOrderConstant.RefundStatus.NO_APPROVE == result.getStatus()){
            workOrderRefundFacade.update(workOrderRefundVo);
            return ResultData.successed(workOrderCode);
        }
        return ResultData.failed("工单状态已变更，不可修改");

    }

    /**
     * 补充物流信息
     * @param workOrderRefundVo
     * @return
     */
    @RequestMapping(value = "/refund/express", method = RequestMethod.POST)
    public ResultData addExpress(@RequestBody WorkOrderRefundVo workOrderRefundVo){
        Integer userId = UserHandleUtil.getUserId();
        logger.info("addExpress - userId:{},workOrderRefund:{}", userId, workOrderRefundVo);
        if (StringUtils.isBlank(workOrderRefundVo.getWaybillNum().trim())){
            return ResultData.failed("运单号错误");
        }
        // 售后工单号
        String workOrderCode = workOrderRefundVo.getWorkOrderCode();
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderCode);
        WorkOrderRefundVo preWorkOrderRefundVo = refundVoResponse.getBody();
        Integer status = preWorkOrderRefundVo.getStatus();
        // 判断该工单状态（等待买家退货 或者 等待卖家收货 ）
        if (status != WorkOrderConstant.RefundStatus.ONE_SUCCESS && status != WorkOrderConstant.RefundStatus.RETURN_GOODS) {
            return ResultData.failed("工单信息已变更，详情请查看售后管理。");
        }
        if(status == WorkOrderConstant.RefundStatus.ONE_SUCCESS) {
            // 审批流ID
            Integer approveId = preWorkOrderRefundVo.getApproveId();
            // 审批通过
            workFlowAdoptOrReject(WorkOrderConstant.ApproveOperType.ADOPT, approveId, null);
        }
        // 设置状态退货中
        workOrderRefundVo.setStatus(WorkOrderConstant.RefundStatus.RETURN_GOODS);
        // 操作人ID
        workOrderRefundVo.setOperatorId(userId);
        // 其他快递，编码为空
        if(StringUtils.isBlank(workOrderRefundVo.getExpressCode())){
            workOrderRefundVo.setExpressCode(ExpressConstant.OTHER_EXPRESS_CODE);
        }
        workOrderRefundFacade.createRefundEntryOrderAndUpdate(workOrderRefundVo);
        return ResultData.successed(workOrderCode);
    }


    /**
     * 同意或者拒绝审批
     * @param operationType 操作类型
     * @param approveId 审批流ID
     * @param refundType 退款类型 1：仅退款 2：退货退款
     */
    private void workFlowAdoptOrReject(Integer operationType, Integer approveId,Integer refundType){
        WorkFlowApplyCondition workFlowApplyCondition = new WorkFlowApplyCondition();
        workFlowApplyCondition.setUserId(UserSessionHandler.getId());
        workFlowApplyCondition.setWrId(approveId);
        if(refundType!=null) {
            workFlowApplyCondition.setModelJson(JsonUtil.objToJson(new RefundTypeModel(refundType)));
        }
        if(operationType.equals(WorkOrderConstant.ApproveOperType.ADOPT)){
            workFlowApplyService.adopt(workFlowApplyCondition);
        }
        else{
            workFlowApplyService.reject(workFlowApplyCondition);
        }
    }

    /**
     * 退款类型model，用于更新审批流传参
     */
    private class RefundTypeModel{

        public RefundTypeModel(Integer refundType){
            this.refundType = refundType;
        }

        private Integer refundType;

        public Integer getRefundType() {
            return refundType;
        }

        public void setRefundType(Integer refundType) {
            this.refundType = refundType;
        }
    }

    /**
     * 工单类型统计
     * @return
     */
    @RequestMapping(value = "/typeStatistics", method = RequestMethod.GET)
    public ResultData queryTypeStatistics(){
        ApiResponse<List<WorkOrderTypeStatisticsVo>> apiResponse = workOrderRefundFacade.queryTypeStatistics();
        List<WorkOrderTypeStatisticsVo> typeStatisticsVos = apiResponse.getBody();
        return ResultData.successed(typeStatisticsVos);
    }
}
