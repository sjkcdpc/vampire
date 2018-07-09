package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.WorkOrderManager;
import com.gaosi.api.basicdata.DistrictApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.common.util.CollectionUtils;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.dragonball.constants.ApprovalConstant;
import com.gaosi.api.dragonball.model.co.WorkFlowApplyCondition;
import com.gaosi.api.dragonball.service.WorkFlowApplyService;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.constant.WorkOrderConstant;
import com.gaosi.api.revolver.dto.QueryWorkOrderRefundDto;
import com.gaosi.api.revolver.facade.ExpressServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.facade.WorkOrderRefundFacade;
import com.gaosi.api.revolver.model.Express;
import com.gaosi.api.revolver.model.WorkOrderRefund;
import com.gaosi.api.revolver.util.JsonUtil;
import com.gaosi.api.revolver.vo.*;
import com.gaosi.api.vulcan.constant.GoodsExtConstant;
import com.gaosi.api.vulcan.facade.GoodsExtServiceFacade;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.facade.GoodsTypeServiceFacade;
import com.gaosi.api.vulcan.facade.MallItemServiceFacade;
import com.gaosi.api.vulcan.model.Goods;
import com.gaosi.api.vulcan.model.GoodsExt;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.model.MallItemPic;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.MallItemVo;
import com.gaosi.api.vulcan.vo.MallSkuVo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
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
    private GoodsServiceFacade goodsServiceFacade;
    @Resource
    private WorkOrderManager workOrderManager;
    @Resource
    private ExpressServiceFacade expressServiceFacade;
    @Resource
    private WorkFlowApplyService workFlowApplyService;
    @Resource
    private DistrictApi districtApi;
    @Resource
    private GoodsExtServiceFacade goodsExtServiceFacade;
    @Resource
    private MallItemServiceFacade mallItemServiceFacade;

    /**
     * 售后工单列表查询
     * @return
     */
    @RequestMapping(value = "/refund", method = RequestMethod.GET)
    public ResultData queryRefundList(QueryWorkOrderRefundDto queryWorkOrderRefundDto){
        if (queryWorkOrderRefundDto == null){
            return ResultData.failed("查询条件不能为空");
        }
        ApiResponse<Page<WorkOrderRefundVo>> pageResponse = workOrderRefundFacade.queryRefundVoList(queryWorkOrderRefundDto);
        Page<WorkOrderRefundVo> page = pageResponse.getBody();
        //总数为0就不进行其他操作了
        if (page.getItemTotal() == 0){
            return ResultData.successed(page);
        }
        List<WorkOrderRefundVo> workOrderRefundVos = page.getList();
        workOrderManager.dealWorkOrderRefundVos(workOrderRefundVos);
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
        ApiResponse<AfterSalesTemplateVo> afterSalesResponse = workOrderRefundFacade.applyAfterSales(oldOrderId, mallSkuId);
        AfterSalesTemplateVo salesTemplateVo = afterSalesResponse.getBody();
        ApiResponse<MallItemVo> mallItemVoResponse = mallItemServiceFacade.findMallItemVoById(salesTemplateVo.getMallItemId());
        MallItemVo mallItemVo = mallItemVoResponse.getBody();
        // 商品名称
        salesTemplateVo.setName(mallItemVo.getName());
        // 商品图片
        List<MallItemPic> mallItemPics = mallItemVo.getMallItemPics();
        if(CollectionUtils.isNotEmpty(mallItemPics)) {
            salesTemplateVo.setPicUrl(mallItemPics.get(0).getPicUrl());
        }
        // 商品规格名称
        List<MallSkuVo> mallSkuVos = mallItemVo.getMallSkuVos();
        for (MallSkuVo mallSkuVo : mallSkuVos) {
            if(mallSkuVo.getId().equals(mallSkuId)){
                salesTemplateVo.setSkuName(mallSkuVo.getName());
            }
        }
        return ResultData.successed(salesTemplateVo);
    }

    /**
     * 提交退款申请
     * @param workOrderRefundVo
     * @return
     */
    @RequestMapping(value = "/refund", method = RequestMethod.POST)
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
        String oldOrderId = workOrderRefundVo.getOldOrderId();
        if (oldOrderId.contains(OrderConstant.SUB_ORDER_ID_FLAG)) {
            oldOrderId = oldOrderId.substring(0,oldOrderId.indexOf(OrderConstant.SUB_ORDER_ID_FLAG));
        }
        // 查询原始订单信息
        ApiResponse<GoodsOrderVo> goodsOrderVoResponse = orderServiceFacade.getGoodsOrderById(oldOrderId);
        GoodsOrderVo goodsOrderVo = goodsOrderVoResponse.getBody();
        workOrderRefundVo.setAreaId(goodsOrderVo.getAreaId());
        // 地址信息
        ApiResponse<AddressDTO> addressResponse = districtApi.getAncestryById(workOrderRefundVo.getAreaId());
        AddressDTO address = addressResponse.getBody();
        workOrderRefundVo.setProvinceId(address.getProvinceId());
        // 创建退货审批流
        Integer approveId = createWorkFlow(workOrderRefundVo.getApproveType(), workOrderRefundVo.getType());
        workOrderRefundVo.setApproveId(approveId);
        workOrderRefundVo.setCreatorId(UserSessionHandler.getId());
        // 查询商品详情
        Map<Integer, Goods> goodsMap = queryGoods(workOrderRefundDetailVos);
        // 查询商品规格详情
        Map<Integer, GoodsType> goodsTypeMap = queryGoodsTypes(workOrderRefundDetailVos);
        // 填充重量，商品ID
        workOrderManager.dealWorkOrderRefundVo(workOrderRefundVo,goodsMap,goodsTypeMap);
        // 创建退款工单
        ApiResponse<String> apiResponse = workOrderRefundFacade.create(workOrderRefundVo);
        return ResultData.successed(apiResponse.getBody());
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
        Set<Integer> goodsTypeIds = CollectionCommonUtil.getFieldSetByObjectList(workOrderRefundDetailVos,
                "getGoodTypeId", Integer.class);
        ApiResponse<List<GoodsType>> goodsTypeResponse = goodsTypeServiceFacade.findGoodsTypeByIds(Lists.newArrayList(goodsTypeIds));
        return CollectionCommonUtil.toMapByList(goodsTypeResponse.getBody(), "getId", Integer.class);
    }

    /**
     * 查询商品信息
     * @param workOrderRefundDetailVos
     * @return
     */
    private Map<Integer, Goods> queryGoods(List<WorkOrderRefundDetailVo> workOrderRefundDetailVos){
        Set<Integer> goodsIds = CollectionCommonUtil.getFieldSetByObjectList(workOrderRefundDetailVos,
                "getGoodsId", Integer.class);
        ApiResponse<List<Goods>> goodsResponse = goodsServiceFacade.queryGoodsByIds(Lists.newArrayList(goodsIds));
        return CollectionCommonUtil.toMapByList(goodsResponse.getBody(), "getId", Integer.class);
    }

    /**
     * 查看退款工单
     * @param workOrderCode
     * @return
     */
    @RequestMapping(value = "/refund/{workOrderCode}", method = RequestMethod.GET)
    public ResultData queryRefundDetail(@PathVariable String workOrderCode){
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderCode);
        WorkOrderRefundVo workOrderRefundVo = refundVoResponse.getBody();
        // 默认没有编辑权限
        workOrderRefundVo.setEditAuth(Boolean.FALSE);
        // 获取当前用户id
        Integer userId = UserSessionHandler.getId();
        Integer creatorId = workOrderRefundVo.getCreatorId();
        // 工单处于未审批，而且当前用户是申请人时，有编辑权限
        if (WorkOrderConstant.RefundStatus.NO_APPROVE == workOrderRefundVo.getStatus() && Objects.equals(userId, creatorId)){
            workOrderRefundVo.setEditAuth(Boolean.TRUE);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("detail", workOrderRefundVo);
        // 判断是不是需要返回物流公司信息
        boolean hasExpress = workOrderManager.hasExpress(workOrderRefundVo);
        if (hasExpress){
            ApiResponse<List<Express>> expressResponse = expressServiceFacade.queryAllExpress();
            List<Express> expressList = expressResponse.getBody();
            Express ext  = new Express();
            ext.setId(0);
            ext.setCode("qita");
            ext.setName("其他");
            expressList.add(ext);
            map.put("express", expressList);
        }
        return ResultData.successed(map);
    }

    /**
     * 修改退货工单
     * @param workOrderRefundVo
     * @return
     */
    @RequestMapping(value = "/refund", method = RequestMethod.PUT)
    public ResultData updateRefund(@RequestBody WorkOrderRefundVo workOrderRefundVo){
        Integer userId = UserSessionHandler.getId();
        logger.info("update refund - userId:{}, workOrderRefund:{}", userId, workOrderRefundVo);
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderRefundVo.getWorkOrderCode());
        WorkOrderRefundVo result = refundVoResponse.getBody();
        // 工单处于未审批，而且当前用户是申请人时，有编辑权限
        if (WorkOrderConstant.RefundStatus.NO_APPROVE == result.getStatus() && Objects.equals(userId, result.getCreatorId())){
            workOrderRefundFacade.update(workOrderRefundVo);
            return ResultData.successed(workOrderRefundVo.getWorkOrderCode());
        }
        return ResultData.failed("工单状态已变更，不可修改");

    }

    /**
     * 补充物流信息
     * @param workOrderRefund
     * @return
     */
    @RequestMapping(value = "/refund/express", method = RequestMethod.PUT)
    public ResultData addExpress(@RequestBody WorkOrderRefund workOrderRefund){
        logger.info("addExpress - userId:{},workOrderRefund:{}",UserSessionHandler.getId(), workOrderRefund);
        if (StringUtils.isBlank(workOrderRefund.getWaybillNum().trim())){
            return ResultData.failed("运单号错误");
        }
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderRefund.getWorkOrderCode());
        WorkOrderRefundVo workOrderRefundVo = refundVoResponse.getBody();
        // 判断该工单是否需要补充物流信息（类型：退货退款，状态：一级审批通过）
        boolean needExpress = workOrderRefundVo.getType().equals(WorkOrderConstant.DetailType.RETURN_GOODS_REFUND) &&
                workOrderRefundVo.getStatus().equals(WorkOrderConstant.RefundStatus.ONE_SUCCESS);
        if (!needExpress){
            return ResultData.failed("工单信息已变更，详情请查看售后管理。");
        }
        // 审批通过
        workFlowAdoptOrReject(WorkOrderConstant.ApproveOperType.ADOPT,workOrderRefund.getApproveId(),null);
        // 设置状态退货中
        workOrderRefund.setStatus(WorkOrderConstant.RefundStatus.RETURN_GOODS);
        workOrderRefundFacade.createRefundEntryOrderAndUpdate(workOrderRefund);
        return ResultData.successed(workOrderRefund.getWorkOrderCode());
    }

    /**
     * 退款工单关闭
     * @return
     */
    @RequestMapping(value = "/refund/close", method = RequestMethod.PUT)
    public ResultData closeRefund(@RequestBody WorkOrderRefundVo workOrderRefundVo){
        logger.info("closeRefund - userId:{}, workOrderRefundVo:{}", UserSessionHandler.getId(), workOrderRefundVo);
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderRefundVo.getWorkOrderCode());
        WorkOrderRefundVo result = refundVoResponse.getBody();
        if (!result.getStatus().equals(WorkOrderConstant.RefundStatus.RETURN_GOODS_COMPLETED)){
            // 非退货完成状态直接返回
            return ResultData.failed("工单状态已变更，请刷新");
        }
        // 结束审批流
        workFlowAdoptOrReject(WorkOrderConstant.ApproveOperType.ADOPT,workOrderRefundVo.getApproveId(),null);
        // 修改状态为已关闭
        workOrderRefundVo.setStatus(WorkOrderConstant.RefundStatus.CLOSED);
        workOrderRefundFacade.update(workOrderRefundVo);
        return ResultData.successed(workOrderRefundVo.getWorkOrderCode());
    }

    /**
     * 退款工单审批
     * @return
     */
    @RequestMapping(value = "/refund/approve", method = RequestMethod.PUT)
    public ResultData approve(@RequestBody WorkOrderRefundVo workOrderRefundVo){
        logger.info("approve - userId:{}, workOrderRefundVo:{}", UserSessionHandler.getId(), workOrderRefundVo);
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderRefundVo.getWorkOrderCode());
        WorkOrderRefundVo result = refundVoResponse.getBody();
        if (!result.getStatus().equals(WorkOrderConstant.RefundStatus.NO_APPROVE)){
            // 非待审批状态直接返回
            return ResultData.failed("工单状态已变更，请刷新");
        }
        Integer operationType ;
        switch (workOrderRefundVo.getStatus()){
            case WorkOrderConstant.RefundStatus.ONE_SUCCESS:
                // 一级审批同意
                operationType = WorkOrderConstant.ApproveOperType.ADOPT;
                break;
            case WorkOrderConstant.RefundStatus.ONE_FAIL:
                // 一级审批拒绝
                operationType = WorkOrderConstant.ApproveOperType.REJECT;
                break;
            default:
                return ResultData.failed("审批状态有误");
        }
        //同意或者拒绝审批
        workFlowAdoptOrReject(operationType,workOrderRefundVo.getApproveId(),workOrderRefundVo.getType());
        //更新工单状态
        workOrderRefundFacade.update(workOrderRefundVo);
        return ResultData.successed(workOrderRefundVo.getWorkOrderCode());
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
     * 查看退款质检结果
     * @param workOrderCode
     * @return
     */
    @RequestMapping(value = "/refund/money/{workOrderCode}", method = RequestMethod.GET)
    public ResultData queryRefundMoney(@PathVariable String workOrderCode){
        ApiResponse<WorkOrderRefundVo> refundVoResponse = workOrderRefundFacade.queryRefundVo(workOrderCode);
        WorkOrderRefundVo workOrderRefundVo = refundVoResponse.getBody();
        Map<String, Object> map = new HashMap<>();
        map.put("result", workOrderRefundVo);
        return ResultData.successed(map);
    }
}
