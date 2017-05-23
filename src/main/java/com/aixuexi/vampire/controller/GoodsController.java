package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.GoodsService;
import com.gaosi.api.revolver.vo.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by zhaowenlei on 17/5/22.
 */
@RestController
@RequestMapping(value = "/goods")
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    /**
     * 获取学科列表
     *
     * @return
     */
    @RequestMapping(value = "/getSubject")
    public ResultData getSubject(){
        ResultData resultData = new ResultData();
        ApiResponse<List<CommonConditionVo>> response = goodsService.querySubject();
        List<CommonConditionVo> subjectVos = response.getBody();

        resultData.setBody(subjectVos);
        return resultData;
    }

    /**
     * 获取学期列表
     *
     * @param subjectId
     * @return
     */
    @RequestMapping(value = "/getPeriod")
    public ResultData getPeriod(Integer subjectId){
        ResultData resultData = new ResultData();
        ApiResponse<List<CommonConditionVo>> response = goodsService.queryPeriod(subjectId);
        List<CommonConditionVo> periodVos = response.getBody();
        resultData.setBody(periodVos);
        return resultData;
    }

    /**
     * 获取分类
     *
     * @param subjectId
     * @param period
     * @return
     */
    @RequestMapping(value = "/getCategory")
    public ResultData getCategory(Integer subjectId, Integer period){
        ResultData resultData = new ResultData();
        ApiResponse<List<CommonConditionVo>> response = goodsService.queryCategory(subjectId, period);
        List<CommonConditionVo> categoryVos = response.getBody();
        resultData.setBody(categoryVos);
        return resultData;
    }

    /**
     * 获取夹菜版本或者考区
     *
     * @param subjectId
     * @param period
     * @param categoryId
     * @return
     */
    @RequestMapping(value = "/getBookVersionArea")
    public ResultData getBookVersionArea(Integer subjectId, Integer period,
                                         Integer categoryId){
        ResultData resultData = new ResultData();
        ApiResponse<List<CommonConditionVo>> response = goodsService.queryBookVersionArea(subjectId,
                period, categoryId);
        List<CommonConditionVo> bookVersionAreaVos = response.getBody();
        resultData.setBody(bookVersionAreaVos);

        return resultData;
    }
}
