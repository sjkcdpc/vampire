package com.aixuexi.vampire.manager;

import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davinciNew.service.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.bo.UserBo;
import com.gaosi.api.dragonball.model.bo.ApprovalAuthorityBo;
import com.gaosi.api.dragonball.service.WorkFlowApplyService;
import com.gaosi.api.revolver.constant.WorkOrderConstant;
import com.gaosi.api.revolver.vo.WorkOrderRefundDetailVo;
import com.gaosi.api.revolver.vo.WorkOrderRefundVo;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.model.Goods;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.gaosi.api.revolver.constant.WorkOrderConstant.RefundStatus.*;
import static com.gaosi.api.revolver.constant.WorkOrderConstant.WorkOrderButton.*;

/**
 * @author liuxinyun
 * @date 2018/1/2 15:28
 * @description
 */
@Service("workOrderManager")
public class WorkOrderManager {

    @Resource
    private InstitutionService institutionService;
    @Resource
    private UserService newUserService;
    @Resource
    private WorkFlowApplyService workFlowApplyService;

    /**
     * 处理退货工单
     * @param workOrderRefundVos
     */
    public void dealWorkOrderRefundVos(List<WorkOrderRefundVo> workOrderRefundVos) {
        // 批量查询用户的审批权限
        Map<Integer, ApprovalAuthorityBo> authorityMap = workFlowCheckAuthority(workOrderRefundVos);
        // 批量查询机构信息
        Map<Integer,Institution> institutionsMap = queryInstitutions(workOrderRefundVos);
        // 批量查询用户信息
        Map<Integer, UserBo> userBoMap = queryUserInfo(workOrderRefundVos);
        // 设置申请人名称,机构名称
        for(WorkOrderRefundVo workOrderRefundVo:workOrderRefundVos){
            workOrderRefundVo.setCreator(userBoMap.get(workOrderRefundVo.getCreatorId()).getName());
            Boolean appoveAuth = Boolean.FALSE;
            if (authorityMap.containsKey(workOrderRefundVo.getApproveId())){
                appoveAuth = authorityMap.get(workOrderRefundVo.getApproveId()).getFlag();
            }
            workOrderRefundVo.setButtonType(queryButtonType(appoveAuth,workOrderRefundVo.getStatus(),
                    workOrderRefundVo.getApproveType(),workOrderRefundVo.getType()));
            for(WorkOrderRefundDetailVo workOrderRefundDetailVo :workOrderRefundVo.getWorkOrderRefundDetailVos()){
                workOrderRefundDetailVo.setInsName(institutionsMap.get(workOrderRefundDetailVo.getInstitutionId()).getName());
            }
        }
    }

    /**
     * 获取按钮名称对应类型
     * @param approveAuth 是否有审批权限
     * @param status 工单状态
     * @param approveType 审批类型
     * @param refundType 退款类型
     * @return
     */
    private Integer queryButtonType(Boolean approveAuth, Integer status, Integer approveType, Integer refundType){
        WorkOrderConstant.WorkOrderButton button = DEFAULT;
        if (approveAuth) {
            switch (status) {
                case NO_APPROVE:
                    // 待审批=》审批
                    button = APPROVE;
                    break;
                case ONE_SUCCESS:
                    if (refundType == WorkOrderConstant.DetailType.ONLY_REFUND) {
                        // 审批类型是一级审批，退款类型是仅退款=》退款
                        button = REFUND;
                    } else {
                        // 审批类型是一级审批，退款类型是退货退款=》补充退货物流
                        button = FILL_EXPRESS;
                    }
                    break;
                case RETURN_GOODS_COMPLETED:
                    // 退货已完成=》退款
                    button = REFUND;
                    break;
                default:
                    break;
            }
        }
        return button.getValue();
    }

    /**
     * 批量查询用户的审批权限
     * @param workOrderRefundVos
     * @return
     */
    private Map<Integer, ApprovalAuthorityBo> workFlowCheckAuthority(List<WorkOrderRefundVo> workOrderRefundVos){
        List<Integer> approveIds = CollectionCommonUtil.getFieldListByObjectList(workOrderRefundVos,
                "getApproveId", Integer.class);
        ApiResponse<List<ApprovalAuthorityBo>> authorityResponse = workFlowApplyService.checkUserAuthority(
                approveIds, UserSessionHandler.getId());
        List<ApprovalAuthorityBo> authoritys = authorityResponse.getBody();
        return CollectionCommonUtil.toMapByList(authoritys, "getWrId", Integer.class);
    }

    /**
     * 批量查询机构信息
     * @param workOrderRefundVos
     * @return
     */
    private Map<Integer,Institution> queryInstitutions(List<WorkOrderRefundVo> workOrderRefundVos){
        List<Integer> institutionIds = Lists.newArrayList(CollectionCommonUtil.getFieldSetByObjectList(workOrderRefundVos,
                "getInstitutionId", Integer.class));
        List<Institution> institutions = institutionService.getByIds(institutionIds);
        return CollectionCommonUtil.toMapByList(institutions, "getId",Integer.class);
    }

    /**
     * 批量查询用户信息
     * @param workOrderRefundVos
     * @return
     */
    private Map<Integer, UserBo> queryUserInfo(List<WorkOrderRefundVo> workOrderRefundVos){
        List<Integer> operatorIds = Lists.newArrayList(CollectionCommonUtil.getFieldSetByObjectList(workOrderRefundVos,
                "getCreatorId", Integer.class));
        ApiResponse<List<UserBo>> operatorResponse = newUserService.findByIdsWithoutRolename(operatorIds);
        List<UserBo> userBoList = operatorResponse.getBody();
        return CollectionCommonUtil.toMapByList(userBoList, "getId", Integer.class);
    }

    /**
     *  填充重量，商品ID
     * @param workOrderRefundVo
     */
    public void dealWorkOrderRefundVo(WorkOrderRefundVo workOrderRefundVo, Map<Integer, Goods> goodsMap, Map<Integer, GoodsType> goodsTypeMap) {
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundVo.getWorkOrderRefundDetailVos()) {
            Integer goodsTypeId = workOrderRefundDetailVo.getGoodTypeId();
            Integer goodsId = workOrderRefundDetailVo.getGoodsId();
            if (goodsTypeMap != null && goodsTypeMap.containsKey(goodsTypeId)) {
                GoodsType goodsType = goodsTypeMap.get(goodsTypeId);
                workOrderRefundDetailVo.setWeight(goodsType.getWeight());
            }
            if (goodsMap != null && goodsMap.containsKey(goodsId)) {
                workOrderRefundDetailVo.setMallItemId(goodsMap.get(goodsId).getMallItemId());
            }
        }
    }

    /**
     * 判断该用户是否有补充物流信息的权限
     * @param workOrderRefundVo
     * @return
     */
    public boolean hasExpress(WorkOrderRefundVo workOrderRefundVo){
        if (workOrderRefundVo.getType() == WorkOrderConstant.DetailType.ONLY_REFUND){
            return false;
        }
        boolean flag = false;
        if (workOrderRefundVo.getStatus() == WorkOrderConstant.RefundStatus.ONE_SUCCESS){
            Map<Integer, ApprovalAuthorityBo> authorityMap = workFlowCheckAuthority(Collections.singletonList(workOrderRefundVo));
            if (authorityMap.containsKey(workOrderRefundVo.getApproveId())){
                flag = authorityMap.get(workOrderRefundVo.getApproveId()).getFlag();
            }
        }
        return flag;
    }

}
