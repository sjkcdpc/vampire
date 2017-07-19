package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.*;
import com.gaosi.api.basicdata.model.bo.*;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.constant.GoodsConstant;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.vo.*;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by zhaowenlei on 17/5/22.
 */
@RestController
@RequestMapping(value = "/goods")
public class GoodsController {

    @Resource
    private GoodsServiceFacade goodsServiceFacade;

    @Resource
    private SubjectProductApi subjectProductApi;

    @Resource
    private ExamAreaApi examAreaApi;

    @Resource
    private BookVersionApi bookVersionApi;

    @Resource
    private DictionaryApi dictionaryApi;

    @Resource
    private SchemeApi schemeApi;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 获取学科列表
     *
     * @return
     */
    @RequestMapping(value = "/getSubject")
    public ResultData getSubject(){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsServiceFacade.querySubject();

        //调用获取名字接口
        List<SubjectProductBo> productBos = subjectProductApi.findSubjectProductList(response.getBody());
        List<CommonConditionVo> conditionVos = baseMapper.mapAsList(productBos, CommonConditionVo.class);
        conditionVos.add(getCommonConditionVo());
        sort(conditionVos);
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
        ApiResponse<List<Integer>> response = goodsServiceFacade.queryPeriod(subjectId);

        //需要调用获取名字接口
        ApiResponse<List<DictionaryBo>> periods = dictionaryApi.findGoodsPeriodByCode(response.getBody());
        List<CommonConditionVo> conditionVos = baseMapper.mapAsList(periods.getBody(), CommonConditionVo.class);
        conditionVos.add(getCommonConditionVo());
        sort(conditionVos);
        resultData.setBody(conditionVos);
        return resultData;
    }

    /**
     * 获取分类
     *
     * @param subjectId
     * @param periodId
     * @return
     */
    @RequestMapping(value = "/getCategory")
    public ResultData getCategory(@RequestParam Integer subjectId,
                                  @RequestParam Integer periodId){
        ResultData resultData = new ResultData();
        ApiResponse<List<CommonConditionVo>> response = goodsServiceFacade.queryCategory(subjectId, periodId);
        List<CommonConditionVo> categoryVos = response.getBody();
        //解决没有考区和没有教材版本的情况
        categoryVos.add(getCommonConditionVo());
        sort(categoryVos);
        resultData.setBody(categoryVos);
        return resultData;
    }

    /**
     * 获取教材版本或者考区
     *
     * @param subjectId
     * @param periodId
     * @param categoryId
     * @return
     */
    @RequestMapping(value = "/getBookVersionArea")
    public ResultData getBookVersionArea(@RequestParam Integer subjectId,
                                         @RequestParam Integer periodId,
                                         @RequestParam Integer categoryId){
        ResultData resultData = new ResultData();
        List<CommonConditionVo> conditionVos = new ArrayList<>();
        if (categoryId.equals(GoodsConstant.GoodsCategory.BOOKVERSION.getValue())) {
            //查询教材版本
            ApiResponse<List<Integer>> response = goodsServiceFacade.queryBookVersion(subjectId, periodId);
            ApiResponse<List<BookVersionBo>> bookVersion = bookVersionApi.findByBookVersionIds(response.getBody());
            conditionVos = baseMapper.mapAsList(bookVersion.getBody(), CommonConditionVo.class);
        }else if (categoryId.equals(GoodsConstant.GoodsCategory.AREA.getValue())) {
            //按考区分
            ApiResponse<List<Integer>> response = goodsServiceFacade.queryArea(subjectId, periodId);
            ApiResponse<List<ExamAreaBo>> examArea = examAreaApi.findByExamAreaIds(response.getBody());
            conditionVos = baseMapper.mapAsList(examArea.getBody(), CommonConditionVo.class);
        }

        conditionVos.add(getCommonConditionVo());
        sort(conditionVos);
        resultData.setBody(conditionVos);

        return resultData;
    }

    /**
     * 通过商品名模糊查找商品
     *
     * @param goodName
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/getByGoodsName")
    public ResultData queryByGoodName(@RequestParam String goodName, @RequestParam Integer pageNum,
                                      @RequestParam Integer pageSize) throws UnsupportedEncodingException {
        Integer insId = UserHandleUtil.getInsId();
        ResultData resultData = new ResultData();
        RequestGoodsConditionVo conditionVo = new RequestGoodsConditionVo();
        conditionVo.setInsId(insId);
        conditionVo.setPageNum(pageNum);
        conditionVo.setPageSize(pageSize);
        conditionVo.setGoodsName(goodName);
        ApiResponse<Page<GoodsVo>> response = goodsServiceFacade.queryGoodsList(conditionVo);
        Page<GoodsVo> page = response.getBody();
        loadRelationName(page.getList(), false);
        resultData.setBody(page);

        return resultData;
    }

    /**
     * 商品列表查询
     *
     * @param sid 学科id
     * @param pid 学期code
     * @param vtId 教材版本=1/考区=2
     * @param veId 教材版本/考区
     * @param pageNum 当前页
     * @param pageSize 页大小
     * @return
     */
    @RequestMapping(value = "/goodsList", method = RequestMethod.GET)
    public ResultData queryGoodsList(@RequestParam Integer sid, @RequestParam Integer pid,
                                     @RequestParam Integer vtId, @RequestParam Integer veId,
                                     @RequestParam Integer pageNum, @RequestParam Integer pageSize){
        Integer insId = UserHandleUtil.getInsId();
        ResultData resultData = new ResultData();
        RequestGoodsConditionVo conditionVo = new RequestGoodsConditionVo();
        conditionVo.setInsId(insId);
        conditionVo.setSid(sid);
        conditionVo.setPid(pid);
        conditionVo.setVtId(vtId);
        conditionVo.setVeId(veId);
        conditionVo.setPageNum(pageNum);
        conditionVo.setPageSize(pageSize);
        ApiResponse<Page<GoodsVo>> response = goodsServiceFacade.queryGoodsList(conditionVo);
        Page<GoodsVo> page = response.getBody();

        loadRelationName(page.getList(), false);
        for(GoodsVo goodsVo:page.getList()) {
            List<GoodsTypeListVo> gtdlist = (List<GoodsTypeListVo>) goodsVo.getGoodsGrades();
            for (GoodsTypeListVo gtdv : gtdlist) {
                String price = gtdv.getPrice();
                Double cny = Double.parseDouble(price);//转换成Double
                DecimalFormat df = new DecimalFormat("0.00");//格式化
                gtdv.setPrice(df.format(cny));
            }
        }
        resultData.setBody(page);
        return resultData;
    }

    /**
     * 商品详情
     *
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public ResultData queryGoodsDetail(@RequestParam Integer goodsId){
        Integer insId = UserHandleUtil.getInsId();
        ResultData resultData = new ResultData();
        ApiResponse<GoodsVo> response = goodsServiceFacade.queryGoodsDetail(goodsId, insId);
        GoodsVo goodsVo = response.getBody();
        goodsVo.setSchemeStr(getScheme(goodsVo.getScheme()));
        List<GoodsTypeDetailVo> gtdlist =(List<GoodsTypeDetailVo>)goodsVo.getGoodsGrades();
        for(GoodsTypeDetailVo gtdv :gtdlist) {
            String price = gtdv.getPrice();
            Double cny = Double.parseDouble(price);//转换成Double
            DecimalFormat df = new DecimalFormat("0.00");//格式化
            gtdv.setPrice(df.format(cny));
        }
        loadRelationName(Lists.newArrayList(goodsVo), true);
        resultData.setBody(response.getBody());
        return resultData;
    }

    private void loadRelationName(List<GoodsVo> list, boolean isDtail){
        List<Integer> bookVersionIds = new ArrayList<>();
        List<Integer> examAreaIds = new ArrayList<>();
        for (GoodsVo goodsVo: list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation:relationGoods) {
                if (!relation.getBookVersion().equals(0)) {
                    bookVersionIds.add(relation.getBookVersion());
                }
                if (!relation.getExamAreaId().equals(0)) {
                    examAreaIds.add(relation.getExamAreaId());
                }
            }
        }
        List<BookVersionBo> bookVersionBos = new ArrayList<>();
        List<ExamAreaBo> examAreaBos = new ArrayList<>();
        Map<Integer, ExamAreaBo> examAreaMap = null;
        Map<Integer, BookVersionBo> bookVersionMap = null;


        if (CollectionUtils.isNotEmpty(bookVersionIds)) {
            ApiResponse<List<BookVersionBo>> bookVersionResponse = bookVersionApi.findByBookVersionIds(bookVersionIds);
            bookVersionBos = bookVersionResponse.getBody();
        }

        if (CollectionUtils.isNotEmpty(examAreaIds)) {
            ApiResponse<List<ExamAreaBo>> examAreaResponse = examAreaApi.findByExamAreaIds(examAreaIds);
            examAreaBos = examAreaResponse.getBody();
        }

        bookVersionMap = toBookVersionMap(bookVersionBos);
        examAreaMap = toExamAreaMap(examAreaBos);

        for (GoodsVo goodsVo: list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation:relationGoods) {
                if (!relation.getBookVersion().equals(0)) {
                    BookVersionBo bookVersion = bookVersionMap.get(relation.getBookVersion());
                    relation.setRelationName(bookVersion.getName());
                    List<?> relationGoodsTypes = relation.getRelationGoodsType();

                    if (CollectionUtils.isNotEmpty(relationGoodsTypes)) {
                        getRelationName(isDtail, relation, relationGoodsTypes);
                    }

                }
                if (!relation.getExamAreaId().equals(0)) {
                    List<?> relationGoodsTypes = relation.getRelationGoodsType();
                    ExamAreaBo examArea = examAreaMap.get(relation.getExamAreaId());
                    relation.setRelationName(examArea.getName());
                    if (CollectionUtils.isNotEmpty(relationGoodsTypes)) {
                        getRelationName(isDtail, relation, relationGoodsTypes);
                    }
                }
            }
        }
    }

    private void getRelationName(boolean isDetail, RelationGoodsVo relation, List<?> relationGoodsTypes){
        if (isDetail) {
            List<GoodsTypeDetailVo> detailVos = baseMapper.mapAsList(relationGoodsTypes, GoodsTypeDetailVo.class);
            relation.setRelationName(relation.getRelationName() + "(");
            int i = 0;
            for (GoodsTypeDetailVo detailVo: detailVos) {
                if (i == 0) {
                    relation.setRelationName(relation.getRelationName() + detailVo.getName());
                }else {
                    relation.setRelationName(relation.getRelationName() + "," + detailVo.getName());
                }
                i++;
            }
            relation.setRelationName(relation.getRelationName() + ")");
        }else {
            List<GoodsTypeListVo> detailVos = baseMapper.mapAsList(relationGoodsTypes, GoodsTypeListVo.class);
            relation.setRelationName(relation.getRelationName() + "(");
            int i = 0;
            for (GoodsTypeListVo detailVo: detailVos) {
                if (i == 0) {
                    relation.setRelationName(relation.getRelationName() + detailVo.getName());
                }else {
                    relation.setRelationName(relation.getRelationName() + "," + detailVo.getName());
                }
                i++;
            }
            relation.setRelationName(relation.getRelationName() + ")");
        }
    }

    private String getScheme(Integer scheme){
        ApiResponse<SchemeBo> response = schemeApi.getById(scheme);
        SchemeBo schemeBo = response.getBody();
        return schemeBo.getName();
    }

    public Map<Integer, BookVersionBo> toBookVersionMap(List<BookVersionBo> bookVersionBos){
        Map<Integer, BookVersionBo> map = new HashMap<>();
        for (BookVersionBo bookVersionBo: bookVersionBos) {
            if (map.containsKey(bookVersionBo.getId())) {
                continue;
            }else {
                map.put(bookVersionBo.getId(), bookVersionBo);
            }
        }

        return map;
    }

    public Map<Integer, ExamAreaBo> toExamAreaMap(List<ExamAreaBo> examAreaBos){
        Map<Integer, ExamAreaBo> map = new HashMap<>();
        for (ExamAreaBo examAreaBo: examAreaBos) {
            if (map.containsKey(examAreaBo.getId())) {
                continue;
            }else {
                map.put(examAreaBo.getId(), examAreaBo);
            }
        }

        return map;
    }

    public CommonConditionVo getCommonConditionVo(){
        CommonConditionVo commonConditionVo = new CommonConditionVo();
        commonConditionVo.setId(0);
        commonConditionVo.setName("全部");
        return commonConditionVo;
    }

    public void sort(List<CommonConditionVo> conditionVos){
        Collections.sort(conditionVos, new Comparator<CommonConditionVo>() {
            @Override
            public int compare(CommonConditionVo o1, CommonConditionVo o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }
}
