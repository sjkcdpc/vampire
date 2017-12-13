package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.util.ApiResponseCheck;
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
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
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
        List<CommonConditionVo> conditionVos = baseMapper.mapAsList(productBos, CommonConditionVo.class);
        conditionVos.add(0, addAllCondition());
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
        for(DictionaryBo db :dictionaryBos) {
            setSortId(db);
        }
        Collections.sort(dictionaryBos, new Comparator<DictionaryBo>() {
            @Override
            public int compare(DictionaryBo o1, DictionaryBo o2) {
                return o1.getOrderIndex().compareTo(o2.getOrderIndex());
            }
        });
        List<CommonConditionVo> conditionVos = baseMapper.mapAsList(dictionaryBos, CommonConditionVo.class);
        conditionVos.add(0, addAllCondition());
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
        categoryVos.add(addAllCondition());
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
            if (bookVersion.getRetCode() != ApiRetCode.SUCCESS_CODE || CollectionUtils.isEmpty(bookVersion.getBody())) {
                logger.error("Get BookVersion Failed ,subjectId :{},periodId : {},categoryId : {}, UserId : {} " +
                        ",InsId : {}", subjectId, periodId, categoryId, UserHandleUtil.getUserId(), UserHandleUtil.getInsId());
                throw new BusinessException(ExceptionCode.UNKNOWN, "查询教材版本失败");
            }
            conditionVos = baseMapper.mapAsList(bookVersion.getBody(), CommonConditionVo.class);
        }else if (categoryId.equals(GoodsConstant.GoodsCategory.AREA.getValue())) {
            //按考区分
            ApiResponse<List<Integer>> response = goodsServiceFacade.queryArea(subjectId, periodId);
            ApiResponse<List<ExamAreaBo>> examArea = examAreaApi.findByExamAreaIds(response.getBody());
            if (examArea.getRetCode() != ApiRetCode.SUCCESS_CODE || CollectionUtils.isEmpty(examArea.getBody())) {
                logger.error("Get AreaVersion Failed ,subjectId :{},periodId : {},categoryId : {}, UserId : {} " +
                        ",InsId : {}", subjectId, periodId, categoryId, UserHandleUtil.getUserId(), UserHandleUtil.getInsId());
                throw new BusinessException(ExceptionCode.UNKNOWN, "查询考区分类失败");
            }
            conditionVos = baseMapper.mapAsList(examArea.getBody(), CommonConditionVo.class);
        }
        conditionVos.add(addAllCondition());
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
        ApiResponseCheck.check(response);
        Page<GoodsVo> page = response.getBody();
        dealGoodsVo(Lists.newArrayList(page.getList()));
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
        ApiResponseCheck.check(response);
        Page<GoodsVo> page = response.getBody();
        dealGoodsVo(Lists.newArrayList(page.getList()));
        resultData.setBody(page);
        return resultData;
    }

    /**
     * 补全商品库存信息
     * @param goodsVoList
     */
    private void loadGoodsInventory(List<GoodsVo> goodsVoList) {
        Set<String> barCodeList = new HashSet<>();
        for (GoodsVo goodsVo : goodsVoList) {
            if (CollectionUtils.isNotEmpty(goodsVo.getGoodsGrades())) {
                List<GoodsTypeCommonVo> typeCommonVos = (List<GoodsTypeCommonVo>) goodsVo.getGoodsGrades();
                barCodeList.addAll(CollectionCommonUtil.getFieldSetByObjectList(typeCommonVos, "getBarCode", String.class));
            }
        }
        if(CollectionUtils.isNotEmpty(barCodeList)) {
            ApiResponse<Map<String, Integer>> apiResponse = invServiceFacade.queryMaxInventory(new ArrayList<>(barCodeList));
            // 查询库存成功
            if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
                Map<String, Integer> invMap = apiResponse.getBody();
                for (GoodsVo goodsVo : goodsVoList) {
                    if (CollectionUtils.isNotEmpty(goodsVo.getGoodsGrades())) {
                        List<GoodsTypeCommonVo> typeCommonVos = (List<GoodsTypeCommonVo>) goodsVo.getGoodsGrades();
                        for (GoodsTypeCommonVo typeCommonVo : typeCommonVos) {
                            typeCommonVo.setGoodsNum(invMap.get(typeCommonVo.getBarCode()));
                        }
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
        ApiResponseCheck.check(response);
        GoodsVo goodsVo = response.getBody();
        if (goodsVo == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "商品不存在!");
        }
        goodsVo.setSchemeStr(getScheme(goodsVo.getScheme()));
        dealGoodsVo(Lists.newArrayList(goodsVo));
        return ResultData.successed(response.getBody());
    }

    /**
     * 补全关联商品的名称
     * @param list
     */
    private void loadRelationName(List<GoodsVo> list) {
        //获取教材版本ID和考区ID的集合
        Set<Integer> bookVersionIds = new HashSet<>();
        Set<Integer> examAreaIds = new HashSet<>();
        for (GoodsVo goodsVo : list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            bookVersionIds.addAll(CollectionCommonUtil.getFieldSetByObjectList(relationGoods,
                    "getBookVersion", Integer.class));
            examAreaIds.addAll(CollectionCommonUtil.getFieldSetByObjectList(relationGoods,
                    "getExamAreaId", Integer.class));
        }
        //排除非教材版本ID和非考区ID
        bookVersionIds.remove(new Integer(0));
        examAreaIds.remove(new Integer(0));
        //获取教材版本和考区的信息
        Map<Integer, ExamAreaBo> examAreaMap = getExamAreaInfo(new ArrayList<>(examAreaIds));
        Map<Integer, BookVersionBo> bookVersionMap = getBookVersionInfo(new ArrayList<>(bookVersionIds));
        //获取关联商品名称
        for (GoodsVo goodsVo : list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation : relationGoods) {
                List<?> relationGoodsTypes = relation.getRelationGoodsType();
                if (bookVersionMap.containsKey(relation.getBookVersion())) {
                    BookVersionBo bookVersion = bookVersionMap.get(relation.getBookVersion());
                    relation.setRelationName(bookVersion.getName());
                }
                if (examAreaMap.containsKey(relation.getExamAreaId())) {
                    ExamAreaBo examArea = examAreaMap.get(relation.getExamAreaId());
                    relation.setRelationName(examArea.getName());
                }
                if (StringUtils.isNotBlank(relation.getRelationName()) &&
                        CollectionUtils.isNotEmpty(relationGoodsTypes)) {
                    combineRelationName(relation, relationGoodsTypes);
                }
            }
        }
    }

    /**
     * 获取教材版本信息
     *
     * @param bookVersionIds
     */
    private Map<Integer, BookVersionBo> getBookVersionInfo(List<Integer> bookVersionIds) {
        Map<Integer, BookVersionBo> bookVersionMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(bookVersionIds)) {
            ApiResponse<List<BookVersionBo>> bookVersionResponse = bookVersionApi.findByBookVersionIds(bookVersionIds);
            ApiResponseCheck.check(bookVersionResponse);
            List<BookVersionBo> bookVersionBos = bookVersionResponse.getBody();
            bookVersionMap = CollectionCommonUtil.toMapByList(bookVersionBos, "getId", Integer.class);
        }
        return bookVersionMap;
    }

    /**
     * 获取考区信息
     *
     * @param examAreaIds
     * @return
     */
    private Map<Integer, ExamAreaBo> getExamAreaInfo(List<Integer> examAreaIds) {
        Map<Integer, ExamAreaBo> examAreaMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(examAreaIds)) {
            ApiResponse<List<ExamAreaBo>> examAreaResponse = examAreaApi.findByExamAreaIds(examAreaIds);
            ApiResponseCheck.check(examAreaResponse);
            List<ExamAreaBo> examAreaBos = examAreaResponse.getBody();
            examAreaMap = CollectionCommonUtil.toMapByList(examAreaBos, "getId", Integer.class);
        }
        return examAreaMap;
    }

    /**
     * 拼接关联商品类型的名称
     * @param relation
     * @param relationGoodsTypes
     */
    private void combineRelationName(RelationGoodsVo relation, List<?> relationGoodsTypes){
        List<GoodsTypeCommonVo> detailVos = baseMapper.mapAsList(relationGoodsTypes, GoodsTypeCommonVo.class);
        // 将"+"形式的字符串拼接替换为stringbuilder
        String preRelationName = relation.getRelationName();

        StringBuilder relationNameBuilder = new StringBuilder();
        relationNameBuilder.append(preRelationName);
        relationNameBuilder.append("(");

        int i = 0;
        for (GoodsTypeCommonVo detailVo: detailVos) {
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

    /**
     * 获取体系名称
     * @param scheme
     * @return
     */
    private String getScheme(Integer scheme){
        ApiResponse<SchemeBo> response = schemeApi.getById(scheme);
        ApiResponseCheck.check(response);
        SchemeBo schemeBo = response.getBody();
        return schemeBo.getName();
    }

    /**
     * 添加全部条件
     * @return
     */
    private CommonConditionVo addAllCondition(){
        CommonConditionVo commonConditionVo = new CommonConditionVo();
        commonConditionVo.setId(0);
        commonConditionVo.setName("全部");
        return commonConditionVo;
    }

    /**
     * 集合类按照ID排序
     * @param conditionVos
     */
    private void sort(List<CommonConditionVo> conditionVos){
        Collections.sort(conditionVos, new Comparator<CommonConditionVo>() {
            @Override
            public int compare(CommonConditionVo o1, CommonConditionVo o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }

    /**
     * 处理GoodsVo
     * @param goodsVoList
     */
    private void dealGoodsVo(List<GoodsVo> goodsVoList){
        loadRelationName(goodsVoList);
        loadGoodsInventory(goodsVoList);
    }
}
