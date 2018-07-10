package com.aixuexi.vampire.manager;

import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davinciNew.service.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.bo.UserBo;
import com.gaosi.api.dragonball.model.bo.ApprovalAuthorityBo;
import com.gaosi.api.dragonball.service.WorkFlowApplyService;
import com.gaosi.api.revolver.constant.WorkOrderConstant;
import com.gaosi.api.revolver.util.WorkOrderUtil;
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
            workOrderRefundVo.setButtonType(WorkOrderUtil.queryButtonType(appoveAuth,workOrderRefundVo.getStatus(),workOrderRefundVo.getType()));
            String insName = institutionsMap.get(workOrderRefundVo.getInstitutionId()).getName();
            for(WorkOrderRefundDetailVo workOrderRefundDetailVo :workOrderRefundVo.getWorkOrderRefundDetailVos()){
                workOrderRefundDetailVo.setInsName(insName);
            }
        }
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
    public void dealWorkOrderRefundVo(WorkOrderRefundVo workOrderRefundVo, Map<Integer, GoodsType> goodsTypeMap) {
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundVo.getWorkOrderRefundDetailVos()) {
            Integer mallSkuId = workOrderRefundDetailVo.getMallSkuId();
            if (goodsTypeMap != null && goodsTypeMap.containsKey(mallSkuId)) {
                GoodsType goodsType = goodsTypeMap.get(mallSkuId);
                workOrderRefundDetailVo.setWeight(goodsType.getWeight());
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
