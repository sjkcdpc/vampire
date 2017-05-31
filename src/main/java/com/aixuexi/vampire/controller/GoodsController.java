package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.gaosi.api.basicdata.BookVersionApi;
import com.gaosi.api.basicdata.DictionaryApi;
import com.gaosi.api.basicdata.ExamAreaApi;
import com.gaosi.api.basicdata.SubjectProductApi;
import com.gaosi.api.basicdata.model.bo.BookVersionBo;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.ExamAreaBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.GoodsConstans;
import com.gaosi.api.revolver.GoodsService;
import com.gaosi.api.revolver.vo.CommonConditionVo;
import com.gaosi.api.revolver.vo.GoodsVo;
import com.gaosi.api.revolver.vo.RequestGoodsConditionVo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaowenlei on 17/5/22.
 */
@RestController
@RequestMapping(value = "/goods")
public class GoodsController {

    @Resource(name = "vGoodsService")
    private GoodsService goodsService;

    @Resource
    private SubjectProductApi subjectProductApi;

    @Resource
    private ExamAreaApi examAreaApi;

    @Resource
    private BookVersionApi bookVersionApi;

    @Resource
    private DictionaryApi dictionaryApi;

    /**
     * 获取学科列表
     *
     * @return
     */
    @RequestMapping(value = "/getSubject")
    public ResultData getSubject(){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsService.querySubject();

        List<CommonConditionVo> conditionVos = new ArrayList<>();
        //调用获取名字接口
        List<SubjectProductBo> productBos = subjectProductApi.findSubjectProductList(response.getBody());
        for (SubjectProductBo productBo: productBos) {
            CommonConditionVo conditionVo = new CommonConditionVo();
            conditionVo.setId(productBo.getSubjectId());
            conditionVo.setName(productBo.getName());
            conditionVos.add(conditionVo);
        }
        resultData.setBody(conditionVos);
        return resultData;
    }

    /**
     * 获取学期列表
     *
     * @param subjectId
     * @return
     */
    @RequestMapping(value = "/getPeriod")
    public ResultData getPeriod(@RequestParam(required = false) Integer subjectId){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsService.queryPeriod(subjectId);
        List<CommonConditionVo> conditionVos = new ArrayList<>();
        //需要调用获取名字接口
        ApiResponse<List<DictionaryBo>> periods = dictionaryApi.findGoodsPeriodByCode(response.getBody());
        for (DictionaryBo dictionaryBo: periods.getBody()) {
            CommonConditionVo conditionVo = new CommonConditionVo();
            conditionVo.setId(Integer.valueOf(dictionaryBo.getCode()));
            conditionVo.setName(dictionaryBo.getName());
            conditionVos.add(conditionVo);
        }

        resultData.setBody(conditionVos);
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
    public ResultData getCategory(@RequestParam(required = false) Integer subjectId,
                                  @RequestParam(required = false) Integer period){
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
    public ResultData getBookVersionArea(@RequestParam(required = false) Integer subjectId,
                                         @RequestParam(required = false) Integer period,
                                         @RequestParam(required = false) Integer categoryId){
        ResultData resultData = new ResultData();
        List<CommonConditionVo> conditionVos = new ArrayList<>();
        if (categoryId.equals(GoodsConstans.GoodsCategory.BOOKVERSION.getValue())) {
            //查询教材版本
            ApiResponse<List<Integer>> response = goodsService.queryBookVersion(subjectId, period);
            ApiResponse<List<BookVersionBo>> bookVersion = bookVersionApi.findByBookVersionIds(response.getBody());
            for (BookVersionBo bookVersionBo: bookVersion.getBody()) {
                CommonConditionVo conditionVo = new CommonConditionVo();
                conditionVo.setId(bookVersionBo.getId());
                conditionVo.setName(bookVersionBo.getName());
                conditionVos.add(conditionVo);
            }
        }else if (categoryId.equals(GoodsConstans.GoodsCategory.AREA.getValue())) {
            //按考区分
            ApiResponse<List<Integer>> response = goodsService.queryArea(subjectId, period);
            ApiResponse<List<ExamAreaBo>> examArea = examAreaApi.findByExamAreaIds(response.getBody());

            for (ExamAreaBo examAreaBo: examArea.getBody()) {
                CommonConditionVo conditionVo = new CommonConditionVo();
                conditionVo.setId(examAreaBo.getId());
                conditionVo.setName(examAreaBo.getName());
                conditionVos.add(conditionVo);
            }
        }

        resultData.setBody(conditionVos);

        return resultData;
    }

    /**
     * 商品列表查询
     *
     * @param insId 结构id
     * @param sid 学科id
     * @param pid 学期code
     * @param vtId 教材版本=1/考区=2
     * @param vid 教材版本
     * @param eid 考区
     * @param pageNum 当前页
     * @param pageSize 页大小
     * @return
     */
    @RequestMapping(value = "/goodsList", method = RequestMethod.GET)
    public ResultData queryGoodsList(@RequestParam Integer insId, @RequestParam(required = false) Integer sid,
                                     @RequestParam(required = false) Integer pid, @RequestParam(required = false) Integer vtId,
                                     @RequestParam(required = false) Integer vid, @RequestParam(required = false) Integer eid,
                                     @RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize){
        ResultData resultData = new ResultData();
        RequestGoodsConditionVo conditionVo = new RequestGoodsConditionVo();
        conditionVo.setInsId(insId);
        conditionVo.setSid(sid);
        conditionVo.setPid(pid);
        conditionVo.setVtId(vtId);
        conditionVo.setVid(vid);
        conditionVo.setEid(eid);
        conditionVo.setPageNum(pageNum);
        conditionVo.setPageSize(pageSize);
        ApiResponse<Page<GoodsVo>> response = goodsService.queryGoodsList(conditionVo);
        Page<GoodsVo> page = response.getBody();
        resultData.setBody(page);

        return resultData;
    }

    /**
     *
     *
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public ResultData queryGoodsDetail(@RequestParam Integer goodsId, @RequestParam Integer insId){
        ResultData resultData = new ResultData();
        ApiResponse<GoodsVo> response = goodsService.queryGoodsDetail(goodsId, insId);
        resultData.setBody(response.getBody());
        return resultData;
    }
}
