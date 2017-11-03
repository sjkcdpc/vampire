package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.exception.BusinessException;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.*;
import com.gaosi.api.basicdata.model.bo.*;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.InvServiceFacade;
import com.gaosi.api.vulcan.constant.GoodsConstant;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.vo.*;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by zhaowenlei on 17/5/22.
 */
@RestController
@RequestMapping(value = "/goods")
public class GoodsController {

    private final Logger logger = LoggerFactory.getLogger(GoodsController.class);

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

    private Map<String,Integer> periodMap = new HashMap<>();

    @Resource
    private InvServiceFacade invServiceFacade;

    @PostConstruct
    private void init(){
        periodMap.put("1",4);
        periodMap.put("2",1);
        periodMap.put("3",2);
        periodMap.put("4",3);
        periodMap.put("5",5);
        periodMap.put("6",6);
    }

    /**
     * 获取学科列表
     *
     * @return
     */
    @RequestMapping(value = "/getSubject",method = RequestMethod.GET)
    public ResultData getSubject(){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsServiceFacade.querySubject();

        //调用获取名字接口
        List<SubjectProductBo> productBos = subjectProductApi.findSubjectProductListForMall(response.getBody());
        //logger.info("sort after : {}",productBos.toString());
        List<CommonConditionVo> conditionVos = baseMapper.mapAsList(productBos, CommonConditionVo.class);
        conditionVos.add(0,getCommonConditionVo());
        //sort(conditionVos);
        resultData.setBody(conditionVos);
        return resultData;
    }

    /**
     * 获取学期列表
     *
     * @param subjectId
     * @return
     */
    @RequestMapping(value = "/getPeriod",method = RequestMethod.GET)
    public ResultData getPeriod(@RequestParam(required = false) Integer subjectId){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsServiceFacade.queryPeriod(subjectId);

        //需要调用获取名字接口
        ApiResponse<List<DictionaryBo>> periods = dictionaryApi.findGoodsPeriodByCode(response.getBody());
        List<DictionaryBo> dictionaryBos = periods.getBody();
        //logger.info("sort before : {} ",dictionaryBos.toString());
        for(DictionaryBo db :dictionaryBos) {
            setSortId(db);
        }
        Collections.sort(dictionaryBos, new Comparator<DictionaryBo>() {
            @Override
            public int compare(DictionaryBo o1, DictionaryBo o2) {
                return o1.getOrderIndex().compareTo(o2.getOrderIndex());
            }
        });
        //logger.info("sort after :{}",dictionaryBos.toString());
        List<CommonConditionVo> conditionVos = baseMapper.mapAsList(dictionaryBos, CommonConditionVo.class);
        conditionVos.add(0,getCommonConditionVo());
        //sort(conditionVos);
        resultData.setBody(conditionVos);
        return resultData;
    }

    /**
     * 学期重置排序
     * @param db
     */
    private void setSortId(DictionaryBo db) {
        String code = db.getCode();
        if(StringUtils.isNotBlank(code)) {
            String trimCode = code.trim();
            if (periodMap.containsKey(trimCode)) {
                db.setOrderIndex(periodMap.get(trimCode));
            } else {
                db.setOrderIndex(db.getId());
            }
        }
    }

    /**
     * 获取分类
     *
     * @param subjectId
     * @param periodId
     * @return
     */
    @RequestMapping(value = "/getCategory",method = RequestMethod.GET)
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
    @RequestMapping(value = "/getBookVersionArea",method = RequestMethod.GET)
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
    @RequestMapping(value = "/getByGoodsName",method = RequestMethod.GET)
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
        loadGoodsInventory(page.getList());
        resultData.setBody(page);
        return resultData;
    }

    /**
     * 补全商品库存信息
     * @param goodsVoList
     */
    private void loadGoodsInventory(List<GoodsVo> goodsVoList) {
        Set<String> barCodeList = new HashSet<>();
        for(GoodsVo goodsVo : goodsVoList){
            if(CollectionUtils.isNotEmpty(goodsVo.getGoodsGrades())) {
                List<GoodsTypeCommonVo> typeCommonVos = (List<GoodsTypeCommonVo>) goodsVo.getGoodsGrades();
                for (GoodsTypeCommonVo typeCommonVo : typeCommonVos) {
                    barCodeList.add(typeCommonVo.getBarCode());
                }
            }
        }
        if(CollectionUtils.isNotEmpty(barCodeList)) {
            ApiResponse<Map<String, Integer>> apiResponse = invServiceFacade.queryMaxInventory(new ArrayList<String>(barCodeList));
            Map<String, Integer> invMap = apiResponse.getBody();
            for (GoodsVo goodsVo : goodsVoList) {
                if(CollectionUtils.isNotEmpty(goodsVo.getGoodsGrades())) {
                    List<GoodsTypeCommonVo> typeCommonVos = (List<GoodsTypeCommonVo>) goodsVo.getGoodsGrades();
                    for (GoodsTypeCommonVo typeCommonVo : typeCommonVos) {
                        typeCommonVo.setGoodsNum(invMap.get(typeCommonVo.getBarCode()));
                    }
                }
            }
        }
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
        ApiResponse<GoodsVo> response = goodsServiceFacade.queryGoodsDetail(goodsId, insId);
        if (response.getRetCode()!= ApiRetCode.SUCCESS_CODE){
            throw new BusinessException(ExceptionCode.UNKNOWN, response.getMessage());
        }
        GoodsVo goodsVo = response.getBody();
        goodsVo.setSchemeStr(getScheme(goodsVo.getScheme()));
        loadRelationName(Lists.newArrayList(goodsVo), true);
        loadGoodsInventory(Lists.newArrayList(goodsVo));
        return ResultData.successed(response.getBody());
    }

    private void loadRelationName(List<GoodsVo> list, boolean isDtail){
        List<Integer> bookVersionIds = new ArrayList<>();
        List<Integer> examAreaIds = new ArrayList<>();
        for (GoodsVo goodsVo : list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation : relationGoods) {
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

        for (GoodsVo goodsVo : list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation : relationGoods) {
                List<?> relationGoodsTypes = relation.getRelationGoodsType();

                Integer bookVersionId = relation.getBookVersion();
                if (!bookVersionId.equals(0)) {
                    BookVersionBo bookVersion = bookVersionMap.get(bookVersionId);
                    relation.setRelationName(bookVersion.getName());

                    if (CollectionUtils.isNotEmpty(relationGoodsTypes)) {
                        getRelationName(isDtail, relation, relationGoodsTypes);
                    }
                }

                Integer examAreaId = relation.getExamAreaId();
                if (!examAreaId.equals(0)) {
                    ExamAreaBo examArea = examAreaMap.get(examAreaId);
                    relation.setRelationName(examArea.getName());
                    if (CollectionUtils.isNotEmpty(relationGoodsTypes)) {
                        getRelationName(isDtail, relation, relationGoodsTypes);
                    }
                }
            }
        }
    }

    private void getRelationName(boolean isDetail, RelationGoodsVo relation, List<?> relationGoodsTypes){
        // 由于有重复代码，用泛型替换
        Class<? extends GoodsTypeCommonVo> clazz = isDetail ? GoodsTypeDetailVo.class : GoodsTypeListVo.class;

        getRelationName(relation, relationGoodsTypes, clazz);
    }

    private <T extends GoodsTypeCommonVo> void getRelationName(RelationGoodsVo relation, List<?> relationGoodsTypes, Class<T> clazz){
        List<T> detailVos = baseMapper.mapAsList(relationGoodsTypes, clazz);
        // 将"+"形式的字符串拼接替换为stringbuilder
        String preRelationName = relation.getRelationName();

        StringBuilder relationNameBuilder = new StringBuilder();
        relationNameBuilder.append(preRelationName);
        relationNameBuilder.append("(");

        int i = 0;
        for (T detailVo: detailVos) {
            String detailName = detailVo.getName();
            if (i != 0) {
                relationNameBuilder.append(",");
            }
            relationNameBuilder.append(detailName);
            i++;
        }
        relationNameBuilder.append(")");
        relation.setRelationName(relationNameBuilder.toString());
    }

    private String getScheme(Integer scheme){
        ApiResponse<SchemeBo> response = schemeApi.getById(scheme);
        SchemeBo schemeBo = response.getBody();
        return schemeBo.getName();
    }

    private Map<Integer, BookVersionBo> toBookVersionMap(List<BookVersionBo> bookVersionBos){
        Map<Integer, BookVersionBo> map = new HashMap<>();

        for (BookVersionBo bookVersionBo : bookVersionBos) {
            if (map.containsKey(bookVersionBo.getId())) {
                continue;
            }
            map.put(bookVersionBo.getId(), bookVersionBo);
        }
        return map;
    }

    private Map<Integer, ExamAreaBo> toExamAreaMap(List<ExamAreaBo> examAreaBos){
        Map<Integer, ExamAreaBo> map = new HashMap<>();

        for (ExamAreaBo examAreaBo : examAreaBos) {
            if (map.containsKey(examAreaBo.getId())) {
                continue;
            }
            map.put(examAreaBo.getId(), examAreaBo);
        }
        return map;
    }

    private CommonConditionVo getCommonConditionVo(){
        CommonConditionVo commonConditionVo = new CommonConditionVo();
        commonConditionVo.setId(0);
        commonConditionVo.setName("全部");
        return commonConditionVo;
    }

    private void sort(List<CommonConditionVo> conditionVos){
        Collections.sort(conditionVos, new Comparator<CommonConditionVo>() {
            @Override
            public int compare(CommonConditionVo o1, CommonConditionVo o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }
}
