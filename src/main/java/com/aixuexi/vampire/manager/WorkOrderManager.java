package com.aixuexi.vampire.manager;

import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.common.util.CollectionUtils;
import com.gaosi.api.davinciNew.service.UserService;
import com.gaosi.api.davincicode.model.UserType;
import com.gaosi.api.davincicode.model.bo.UserBo;
import com.gaosi.api.revolver.constant.WorkOrderConstant;
import com.gaosi.api.revolver.vo.WorkOrderDealRecordVo;
import com.gaosi.api.revolver.vo.WorkOrderRefundDetailVo;
import com.gaosi.api.revolver.vo.WorkOrderRefundVo;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.dto.QueryMallSkuVoDto;
import com.gaosi.api.vulcan.facade.MallSkuServiceFacade;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.model.MallItemPic;
import com.gaosi.api.vulcan.model.MallSkuPic;
import com.gaosi.api.vulcan.vo.MallSkuVo;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private MallSkuServiceFacade mallSkuServiceFacade;

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
        // 补充商品相关的信息
        dealWorkOrderRefundDetailVo(workOrderRefundDetailVos);
    }

    /**
     * 补充协商历史信息
     * @param workOrderDealRecordVos
     */
    public void dealRefundDealRecords(List<WorkOrderDealRecordVo> workOrderDealRecordVos) {
        if(CollectionUtils.isNotEmpty(workOrderDealRecordVos)) {
            // 批量查询用户信息
            Set<Integer> userIds = workOrderDealRecordVos.stream().map(WorkOrderDealRecordVo::getUserId).collect(Collectors.toSet());
            Map<Integer, UserBo> userBoMap = queryUserInfo(userIds);
            for (WorkOrderDealRecordVo workOrderDealRecordVo : workOrderDealRecordVos) {
                if (workOrderDealRecordVo.getUserId() == -1) {
                    // -1表示第三方操作人员，譬如仓库质检，显示爱学习
                    workOrderDealRecordVo.setUserName(WorkOrderConstant.DEAL_RECORD_MANAGER_NAME);
                    workOrderDealRecordVo.setUserPic(WorkOrderConstant.DEAL_RECORD_MANAGER_PIC);
                    continue;
                }
                UserBo user = userBoMap.get(workOrderDealRecordVo.getUserId());
                if (UserType.MANAGE.getValue().equals(user.getUserType())) {
                    workOrderDealRecordVo.setUserName(WorkOrderConstant.DEAL_RECORD_MANAGER_NAME);
                    workOrderDealRecordVo.setUserPic(WorkOrderConstant.DEAL_RECORD_MANAGER_PIC);
                } else {
                    workOrderDealRecordVo.setUserName(user.getName());
                    workOrderDealRecordVo.setUserPic(user.getPortraitPath());
                }
            }
        }
    }

    /**
     * 处理退货工单详情（补充商品相关的信息）
     * @param workOrderRefundDetailVos
     */
    public void dealWorkOrderRefundDetailVo(List<WorkOrderRefundDetailVo> workOrderRefundDetailVos){
        List<Integer> mallSkuIds = workOrderRefundDetailVos.stream().map(WorkOrderRefundDetailVo::getMallSkuId).collect(Collectors.toList());
        QueryMallSkuVoDto queryMallSkuVoDto = new QueryMallSkuVoDto();
        queryMallSkuVoDto.setIds(mallSkuIds);
        queryMallSkuVoDto.setNeedPic(true);
        ApiResponse<List<MallSkuVo>> apiResponse = mallSkuServiceFacade.queryMallSkuVoBySkuIds(queryMallSkuVoDto);
        List<MallSkuVo> mallSkuVoList = apiResponse.getBody();
        Map<Integer, MallSkuVo> mallSkuVoMap = mallSkuVoList.stream().collect(Collectors.toMap(MallSkuVo::getId, m -> m, (k1, k2) -> k1));
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundDetailVos) {
            Integer mallSkuId = workOrderRefundDetailVo.getMallSkuId();
            MallSkuVo mallSkuVo = mallSkuVoMap.get(mallSkuId);
            // 申请售后时商品名称由商品表提供，其他情况显示工单详情中的商品名称
            if(StringUtils.isBlank(workOrderRefundDetailVo.getName())){
                workOrderRefundDetailVo.setName(mallSkuVo.getMallItemName());
            }
            workOrderRefundDetailVo.setSkuName(mallSkuVo.getName());
            workOrderRefundDetailVo.setSkuCode(mallSkuVo.getCode());
            Integer categoryId = mallSkuVo.getCategoryId();
            // 商品图片
            if(categoryId.equals(MallItemConstant.Category.JCSD.getId())){
                List<MallSkuPic> mallSkuPics = mallSkuVo.getMallSkuPics();
                if(CollectionUtils.isNotEmpty(mallSkuPics)){
                    workOrderRefundDetailVo.setPicUrl(mallSkuPics.get(0).getPicUrl());
                }
            }else{
                List<MallItemPic> mallItemPics = mallSkuVo.getMallItemPics();
                if(CollectionUtils.isNotEmpty(mallItemPics)) {
                    workOrderRefundDetailVo.setPicUrl(mallItemPics.get(0).getPicUrl());
                }
            }
        }
    }

    /**
     * 批量查询用户信息
     * @param userIds
     * @return
     */
    private Map<Integer, UserBo> queryUserInfo(Set<Integer> userIds){
        ApiResponse<List<UserBo>> operatorResponse = newUserService.findByIdsWithoutRolename(Lists.newArrayList(userIds));
        List<UserBo> userBoList = operatorResponse.getBody();
        return userBoList.stream().collect(Collectors.toMap(UserBo::getId, u -> u, (k1, k2) -> k1));
    }

    /**
     *  填充重量，商品ID
     * @param workOrderRefundVo
     */
    public void dealWorkOrderRefundVo(WorkOrderRefundVo workOrderRefundVo, Map<Integer, GoodsType> goodsTypeMap) {
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundVo.getWorkOrderRefundDetailVos()) {
            Integer mallSkuId = workOrderRefundDetailVo.getMallSkuId();
            workOrderRefundDetailVo.setWeight(0D);
            if (goodsTypeMap != null && goodsTypeMap.containsKey(mallSkuId)) {
                GoodsType goodsType = goodsTypeMap.get(mallSkuId);
                workOrderRefundDetailVo.setWeight(goodsType.getWeight());
            }
        }
    }
}
