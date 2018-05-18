package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.GoodsManager;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.DictionaryApi;
import com.gaosi.api.basicdata.SubjectProductApi;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.aixuexi.vampire.manager.FinancialAccountManager;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.vo.ItemOrderStatisVo;
import com.gaosi.api.vulcan.bean.common.QueryCriteria;
import com.gaosi.api.vulcan.constant.GoodsTypePriceConstant;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.model.TalentFilterCondition;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.*;
import com.gaosi.api.workorder.facade.TemplateServiceFacade;
import com.gaosi.api.workorder.vo.TemplateFieldVo;
import com.gaosi.api.xmen.constant.DictConstants;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.aixuexi.vampire.util.Constants.RCZX_TEMPLATE_CODE;

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
    private TemplateServiceFacade templateServiceFacade;

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
        ApiResponse<Page<MallItemNailVo>> mallItemNailVoResponse = mallItemExtServiceFacade.queryMallItemNailList(queryCriteria);
        ApiResponseCheck.check(mallItemNailVoResponse);
        Page<MallItemNailVo> mallItemNailVoPage = mallItemNailVoResponse.getBody();
        List<MallItemNailVo> mallItemNailVos = mallItemNailVoPage.getList();
        if (CollectionUtils.isNotEmpty(mallItemNailVos)) {
            List<Integer> ids = CollectionCommonUtil.getFieldListByObjectList(mallItemNailVos,
                    "getMallItemId",Integer.class);
            ApiResponse<List<ItemOrderStatisVo>> itemOrderStatisVoResponse = itemOrderServiceFacade.getCountByItemId(ids);
            ApiResponseCheck.check(itemOrderStatisVoResponse);
            List<ItemOrderStatisVo> signedUpNums = itemOrderStatisVoResponse.getBody();
            Map<Integer, ItemOrderStatisVo> signedUpNumMap = CollectionCommonUtil.toMapByList(signedUpNums,
                    "getItemId", Integer.class);
            for (MallItemNailVo mno : mallItemNailVos) {
                Integer mallItemId = mno.getMallItemId();
                Integer signedUpNum = signedUpNumMap.get(mallItemId).getSignedTotal();
                mno.setSignedUpNum(signedUpNum);
                //添加名额已满的状态
                if (mno.getSignUpNum() > 0 && signedUpNum >= mno.getSignUpNum()) {
                    mno.setSignUpStatus(MallItemConstant.SignUpStatus.NUM_FULL);
                }
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
        ApiResponse<MallItemNailVo> mallItemNailVoResponse = mallItemExtServiceFacade.queryMallItemNailDetail(mallItemId, MallItemConstant.ShelvesStatus.ON);
        ApiResponseCheck.check(mallItemNailVoResponse);
        MallItemNailVo mallItemNailVo = mallItemNailVoResponse.getBody();
        //已报名数量查询
        ApiResponse<List<ItemOrderStatisVo>> itemOrderStatisVoResponse = itemOrderServiceFacade.getCountByItemId(Lists.newArrayList(mallItemNailVo.getMallItemId()));
        ApiResponseCheck.check(itemOrderStatisVoResponse);
        List<ItemOrderStatisVo> itemOrderStatisVos = itemOrderStatisVoResponse.getBody();
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
        ApiResponseCheck.check(apiResponse);
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
        QueryCriteria queryCriteria = new QueryCriteria();
        queryCriteria.setPageNum(pageNum);
        queryCriteria.setPageSize(pageSize);
        queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
        queryCriteria.setCategoryId(MallItemConstant.Category.DZFW.getId());
        ApiResponse<Page<MallItemCustomServiceVo>> apiResponse = mallItemExtServiceFacade.queryMallItemList4DZFW(queryCriteria, UserHandleUtil.getInsId());
        ApiResponseCheck.check(apiResponse);
        return ResultData.successed(apiResponse.getBody());
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
        ApiResponseCheck.check(apiResponse);
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
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        ApiResponse<Page<MallItemTalentVo>> apiResponse = mallItemExtServiceFacade.queryMallItemList4Talent(reqTalentCenterConditionVo);
        ApiResponseCheck.check(apiResponse);
        Page<MallItemTalentVo> mallItemTalentVoPage = apiResponse.getBody();
        List<MallItemTalentVo> mallItemTalentVos = mallItemTalentVoPage.getList();
        dealMallItemTalentVo(mallItemTalentVos);
        return ResultData.successed(mallItemTalentVoPage);
    }

    /**
     * 人才中心详情
     * @param mallItemId
     * @return
     */
    @RequestMapping(value = "/talentCenter/detail", method = RequestMethod.GET)
    public ResultData queryTalentCenterDetail(@RequestParam Integer mallItemId) {
        ReqTalentCenterConditionVo reqTalentCenterConditionVo = new ReqTalentCenterConditionVo();
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setMallItemId(mallItemId);
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        ApiResponse<MallItemTalentVo> apiResponse = mallItemExtServiceFacade.queryMallItem4Talent(reqTalentCenterConditionVo);
        ApiResponseCheck.check(apiResponse);
        MallItemTalentVo mallItemTalentVo = apiResponse.getBody();
        dealMallItemTalentVo(Lists.newArrayList(mallItemTalentVo));
        return ResultData.successed(mallItemTalentVo);
    }

    /**
     * 人才中心订单确认
     * @param mallItemId
     * @param mallSkuId
     * @param num
     * @return
     */
    @RequestMapping(value = "/talentCenter/confirm", method = RequestMethod.GET)
    public ResultData confirmTalentCenter(@RequestParam Integer mallItemId,@RequestParam Integer mallSkuId,
                                          @RequestParam Integer num) {
        ReqTalentCenterConditionVo reqTalentCenterConditionVo = new ReqTalentCenterConditionVo();
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setMallItemId(mallItemId);
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        ApiResponse<ConfirmTalentVo> apiResponse = mallItemExtServiceFacade.confirmTalentCenter(reqTalentCenterConditionVo, mallSkuId, num);
        ApiResponseCheck.check(apiResponse);
        ConfirmTalentVo confirmTalentVo = apiResponse.getBody();
        // 查询账户余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        Double balance = AmountUtil.divide(Double.valueOf(rr.getUsableRemain()),10000D);
        confirmTalentVo.setBalance(balance);
        // 查询工单模板
        com.aixuexi.thor.response.ApiResponse<List<TemplateFieldVo>> templateResponse = templateServiceFacade.queryTemplateFields(RCZX_TEMPLATE_CODE);
        ApiResponseCheck.checkNew(templateResponse);
        List<TemplateFieldVo> templateFieldVos = templateResponse.getBody();
        confirmTalentVo.setTalentTemplate(templateFieldVos);
        return ResultData.successed(confirmTalentVo);
    }

    /**
     * 处理人才中心VO（补充销售量）
     * @param mallItemTalentVos
     */
    private void dealMallItemTalentVo(List<MallItemTalentVo> mallItemTalentVos) {

    }
}
