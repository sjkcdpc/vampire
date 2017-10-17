package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.vo.ItemOrderStatisVo;
import com.gaosi.api.vulcan.bean.common.QueryCriteria;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.ConfirmMallItemNailVo;
import com.gaosi.api.vulcan.vo.MallItemNailVo;
import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

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
    private FinancialAccountService finAccService;

    @RequestMapping(value = "/nail",method = RequestMethod.GET)
    public ResultData queryMallItemNailList(@RequestParam Integer pageNum,@RequestParam Integer pageSize)
    {
        ResultData resultData = new ResultData();
        QueryCriteria queryCriteria = new QueryCriteria();
        queryCriteria.setPageNum(pageNum);
        queryCriteria.setPageSize(pageSize);
        queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
        ApiResponse<Page<MallItemNailVo>> apiResponse= mallItemExtServiceFacade.queryMallItemNailList(queryCriteria);
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            throw new IllegalArgException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        Page<MallItemNailVo> mallItemNailVoPage = apiResponse.getBody();
        List<Integer> ids = new ArrayList<>();
        if(CollectionUtils.isEmpty(mallItemNailVoPage.getList())){
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "未查找到校长培训列表!");
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

    @RequestMapping(value = "/nail/detail",method = RequestMethod.GET)
    public ResultData queryMallItemNailDetail(@RequestParam Integer mallItemId)
    {
        ResultData resultData = new ResultData();
        ApiResponse<MallItemNailVo> apiResponse = mallItemExtServiceFacade.queryMallItemNailDetail(mallItemId,MallItemConstant.ShelvesStatus.ON);
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            throw new IllegalArgException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        MallItemNailVo mallItemNailVo = apiResponse.getBody();
        //已报名数量查询
        ApiResponse<List<ItemOrderStatisVo>> apiResponse1 = itemOrderServiceFacade.getCountByItemId(Lists.newArrayList(mallItemNailVo.getMallItemId()));
        if (apiResponse1.getRetCode() != ApiRetCode.SUCCESS_CODE){
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
    @RequestMapping(value = "/nail/confirm",method = RequestMethod.GET)
    public ResultData  confirmMallItemNail(@RequestParam Integer mallItemId,@RequestParam Integer goodsPieces)
    {
        if(goodsPieces < 1){
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "报名人数错误");
        }
        ResultData resultData = new ResultData();
        ApiResponse<ConfirmMallItemNailVo> apiResponse = mallItemExtServiceFacade.confirmMallItemNail(mallItemId,goodsPieces);
        if (apiResponse.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            throw new IllegalArgException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        ConfirmMallItemNailVo confirmMallItemNailVo = apiResponse.getBody();
        RemainResult rr = finAccService.getRemainByInsId(UserHandleUtil.getInsId());
        if (rr == null) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "账户不存在");
        }
        Double balance = Double.valueOf(rr.getUsableRemain()) / 10000;
        confirmMallItemNailVo.setBalance(balance);

        resultData.setBody(confirmMallItemNailVo);
        return resultData;
    }
}
