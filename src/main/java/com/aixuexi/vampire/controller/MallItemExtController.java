package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.GoodsManager;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.gaosi.api.basicdata.DictionaryApi;
import com.gaosi.api.basicdata.SubjectProductApi;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.aixuexi.vampire.manager.FinancialAccountManager;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.vo.ItemOrderStatisVo;
import com.gaosi.api.vulcan.bean.common.QueryCriteria;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.model.TalentFilterCondition;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.*;
import com.gaosi.api.xmen.constant.DictConstants;
import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ruanyanjie on 2017/7/28.
 */
@RestController
@RequestMapping(value = "/item")
public class MallItemExtController {

    @Resource
    private MallItemExtServiceFacade mallItemExtServiceFacade;

    @Resource
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Resource
    private FinancialAccountManager financialAccountManager;

    @Resource
    private DictionaryApi dictionaryApi;

    @Resource
    private SubjectProductApi subjectProductApi;

    @Resource
    private GoodsManager goodsManager;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 校长培训列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/nail", method = RequestMethod.GET)
    public ResultData queryMallItemNailList(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        ResultData resultData = new ResultData();
        QueryCriteria queryCriteria = new QueryCriteria();
        queryCriteria.setPageNum(pageNum);
        queryCriteria.setPageSize(pageSize);
        queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
        ApiResponse<Page<MallItemNailVo>> apiResponse = mallItemExtServiceFacade.queryMallItemNailList(queryCriteria);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        Page<MallItemNailVo> mallItemNailVoPage = apiResponse.getBody();
        List<Integer> ids = new ArrayList<>();
        if (CollectionUtils.isEmpty(mallItemNailVoPage.getList())) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "未查找到校长培训列表!");
        }
        for (MallItemNailVo mno : mallItemNailVoPage.getList()) {
            ids.add(mno.getMallItemId());
        }
        ApiResponse<List<ItemOrderStatisVo>> signedUpNumList = itemOrderServiceFacade.getCountByItemId(ids);
        List<ItemOrderStatisVo> signedUpNums = signedUpNumList.getBody();
        Map<Integer, ItemOrderStatisVo> signedUpNumMap = CollectionCommonUtil.toMapByList(signedUpNums, "getItemId", Integer.class);
        for (MallItemNailVo mno : mallItemNailVoPage.getList()) {
            Integer mallItemId = mno.getMallItemId();
            Integer signedUpNum = signedUpNumMap.get(mallItemId).getSignedTotal();
            mno.setSignedUpNum(signedUpNum);
            //添加名额已满的状态
            if (mno.getSignUpNum() > 0 && signedUpNum >= mno.getSignUpNum()) {
                mno.setSignUpStatus(MallItemConstant.SignUpStatus.NUM_FULL);
            }
        }
        resultData.setBody(mallItemNailVoPage);
        return resultData;
    }

    /**
     * 校长培训详情
     * @param mallItemId
     * @return
     */
    @RequestMapping(value = "/nail/detail", method = RequestMethod.GET)
    public ResultData queryMallItemNailDetail(@RequestParam Integer mallItemId) {
        ResultData resultData = new ResultData();
        ApiResponse<MallItemNailVo> apiResponse = mallItemExtServiceFacade.queryMallItemNailDetail(mallItemId, MallItemConstant.ShelvesStatus.ON);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        MallItemNailVo mallItemNailVo = apiResponse.getBody();
        //已报名数量查询
        ApiResponse<List<ItemOrderStatisVo>> apiResponse1 = itemOrderServiceFacade.getCountByItemId(Lists.newArrayList(mallItemNailVo.getMallItemId()));
        if (apiResponse1.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            return ResultData.failed("查询已报名数量失败");
        }
        List<ItemOrderStatisVo> itemOrderStatisVos = apiResponse1.getBody();
        Map<Integer, ItemOrderStatisVo> map = CollectionCommonUtil.toMapByList(itemOrderStatisVos, "getItemId", Integer.class);
        int signedUpNum = map.get(mallItemNailVo.getMallItemId()).getSignedTotal();
        mallItemNailVo.setSignedUpNum(signedUpNum);
        //添加名额已满的状态
        if (mallItemNailVo.getSignUpNum() > 0 && signedUpNum >= mallItemNailVo.getSignUpNum()) {
            mallItemNailVo.setSignUpStatus(MallItemConstant.SignUpStatus.NUM_FULL);
        }
        resultData.setBody(mallItemNailVo);
        return resultData;
    }

    /**
     * 校长培训确认订单
     * @param mallItemId
     * @param goodsPieces
     * @return
     */
    @RequestMapping(value = "/nail/confirm", method = RequestMethod.GET)
    public ResultData confirmMallItemNail(@RequestParam Integer mallItemId, @RequestParam Integer goodsPieces) {
        if (goodsPieces < 1) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "报名人数错误");
        }
        ResultData resultData = new ResultData();
        ApiResponse<ConfirmMallItemNailVo> apiResponse = mallItemExtServiceFacade.confirmMallItemNail(mallItemId, goodsPieces);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        ConfirmMallItemNailVo confirmMallItemNailVo = apiResponse.getBody();
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        Double balance = Double.valueOf(rr.getUsableRemain()) / 10000;
        confirmMallItemNailVo.setBalance(balance);

        resultData.setBody(confirmMallItemNailVo);
        return resultData;
    }

    /**
     * 定制服务列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/customService", method = RequestMethod.GET)
    public ResultData queryCustomServiceList(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        ResultData resultData = new ResultData();
        QueryCriteria queryCriteria = new QueryCriteria();
        queryCriteria.setPageNum(pageNum);
        queryCriteria.setPageSize(pageSize);
        queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
        queryCriteria.setCategoryId(MallItemConstant.Category.DZFW.getId());
        ApiResponse<Page<MallItemCustomServiceVo>> apiResponse = mallItemExtServiceFacade.queryMallItemList4DZFW(queryCriteria,UserHandleUtil.getInsId());
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        Page<MallItemCustomServiceVo> customServiceVoPage = apiResponse.getBody();
        if (CollectionUtils.isEmpty(customServiceVoPage.getList())) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "未查找到定制服务列表!");
        }
        resultData.setBody(customServiceVoPage);
        return resultData;
    }

    /**
     * 定制服务确认订单
     * @param mallItemId
     * @param goodsPieces
     * @return
     */
    @RequestMapping(value = "/customService/confirm", method = RequestMethod.GET)
    public ResultData confirmCustomService(@RequestParam Integer mallItemId,@RequestParam Integer goodsPieces ){
        if (goodsPieces < 1) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "商品数量错误");
        }
        ResultData resultData = new ResultData();
        ApiResponse<ConfirmCustomServiceVo> apiResponse = mallItemExtServiceFacade.confirmMallItem4DZFW(mallItemId, goodsPieces);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        ConfirmCustomServiceVo confirmCustomServiceVo = apiResponse.getBody();
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        Double balance = Double.valueOf(rr.getUsableRemain()) / 10000;
        confirmCustomServiceVo.setBalance(balance);

        resultData.setBody(confirmCustomServiceVo);
        return resultData;
    }

    /**
     * 人才中心筛选条件
     * @return
     */
    @RequestMapping(value = "/talentCenter/queryCondition", method = RequestMethod.GET)
    public ResultData queryCondition4TalentCenter(){
        // 全部筛选条件
        List<CommonConditionVo> allCondition = new ArrayList<>();
        // 获取人才中心的筛选条件
        ApiResponse<TalentFilterCondition> talentResponse = mallItemExtServiceFacade.queryTalentFilterCondition();
        ApiResponseCheck.check(talentResponse);
        TalentFilterCondition talentFilterCondition = talentResponse.getBody();
        List<String> typeCodes = talentFilterCondition.getTypeCode();
        List<Integer> subjectProductIds = talentFilterCondition.getSubjectProductId();
        // 查询字典表中的人才类型
        ApiResponse<List<DictionaryBo>> dictionaryResponse = dictionaryApi.findByType(DictConstants.TALENT_TYPE);
        ApiResponseCheck.check(dictionaryResponse);
        List<DictionaryBo> dictionaryBos = dictionaryResponse.getBody();
        List<CommonConditionVo> typeCodeCondition = new ArrayList<>();
        for (DictionaryBo dictionaryBo : dictionaryBos) {
            if(typeCodes.contains(dictionaryBo.getCode())){
                typeCodeCondition.add(new CommonConditionVo(dictionaryBo.getId(),dictionaryBo.getCode(),dictionaryBo.getName()));
            }
        }
        typeCodeCondition.add(0, goodsManager.addAllCondition());
        allCondition.add(new CommonConditionVo(0,"人才类型",typeCodeCondition));
        // 查询基础数据的学科
        List<SubjectProductBo> subjectProductList = subjectProductApi.findSubjectProductList(subjectProductIds);
        List<CommonConditionVo> subjectProductCondition = baseMapper.mapAsList(subjectProductList,CommonConditionVo.class);
        subjectProductCondition.add(0, goodsManager.addAllCondition());
        allCondition.add(new CommonConditionVo(1,"所属学科",subjectProductCondition));
        return ResultData.successed(allCondition);
    }

    /**
     * 人才中心列表
     * @return
     */
    @RequestMapping(value = "/talentCenter/list", method = RequestMethod.GET)
    public ResultData queryTalentCenterList(ReqTalentCenterConditionVo reqTalentCenterConditionVo){
        return null;
    }
}
