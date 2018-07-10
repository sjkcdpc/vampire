package com.aixuexi.vampire.manager;

import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.common.util.CollectionUtils;
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
import com.gaosi.api.vulcan.facade.MallItemServiceFacade;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.model.MallItemPic;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.MallItemVo;
import com.gaosi.api.vulcan.vo.MallSkuVo;
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
    @Resource
    private MallItemServiceFacade mallItemServiceFacade;

    /**
     * 处理退货工单列表
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
     * 处理单个退货工单--查看详情使用
     * @param workOrderRefundVo
     */
    public void dealWorkOrderRefundVo(WorkOrderRefundVo workOrderRefundVo){
        List<WorkOrderRefundDetailVo> workOrderRefundDetailVos = workOrderRefundVo.getWorkOrderRefundDetailVos();
        WorkOrderRefundDetailVo workOrderRefundDetailVo = workOrderRefundDetailVos.get(0);
        // 机构信息
        Integer institutionId = workOrderRefundVo.getInstitutionId();
        Institution institution = institutionService.getInsInfoById(institutionId);
        workOrderRefundDetailVo.setInsName(institution.getName());
        dealWorkOrderRefundDetailVo(workOrderRefundDetailVo);
    }

    /**
     * 处理退货工单详情（补充商品相关的信息）
     * @param workOrderRefundDetailVo
     */
    public void dealWorkOrderRefundDetailVo(WorkOrderRefundDetailVo workOrderRefundDetailVo){
        Integer mallItemId = workOrderRefundDetailVo.getMallItemId();
        Integer mallSkuId = workOrderRefundDetailVo.getMallSkuId();
        ApiResponse<MallItemVo> mallItemVoResponse = mallItemServiceFacade.findMallItemVoById(mallItemId);
        MallItemVo mallItemVo = mallItemVoResponse.getBody();
        // 商品名称
        workOrderRefundDetailVo.setName(mallItemVo.getName());
        // 商品图片
        List<MallItemPic> mallItemPics = mallItemVo.getMallItemPics();
        if(CollectionUtils.isNotEmpty(mallItemPics)) {
            workOrderRefundDetailVo.setPicUrl(mallItemPics.get(0).getPicUrl());
        }
        // 商品规格名称,编码
        List<MallSkuVo> mallSkuVos = mallItemVo.getMallSkuVos();
        for (MallSkuVo mallSkuVo : mallSkuVos) {
            if(mallSkuVo.getId().equals(mallSkuId)){
                workOrderRefundDetailVo.setSkuName(mallSkuVo.getName());
                workOrderRefundDetailVo.setSkuCode(mallSkuVo.getCode());
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
