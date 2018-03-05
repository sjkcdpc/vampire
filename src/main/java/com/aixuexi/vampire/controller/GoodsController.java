package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.GoodsManager;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.*;
import com.gaosi.api.basicdata.model.bo.BookVersionBo;
import com.gaosi.api.basicdata.model.bo.ExamAreaBo;
import com.gaosi.api.basicdata.model.bo.SchemeBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.InvServiceFacade;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.model.GoodsFilterCondition;
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
    private ExamAreaApi examAreaApi;

    @Resource
    private BookVersionApi bookVersionApi;

    @Resource
    private SchemeApi schemeApi;

    @Resource
    private BaseMapper baseMapper;

    @Resource
    private InvServiceFacade invServiceFacade;

    @Resource(name = "goodsManager")
    private GoodsManager goodsManager;

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
        ResultData resultData = new ResultData();
        ReqGoodsConditionVo conditionVo = new ReqGoodsConditionVo();
        conditionVo.setInstitutionId(UserHandleUtil.getInsId());
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
     * 获取科目品牌以及学科的筛选条件
     * @return
     */
    @RequestMapping(value = "/partQueryCondition", method = RequestMethod.GET)
    public ResultData partQueryCondition(){
        // 获取教材筛选条件的ID集合
        ApiResponse<GoodsFilterCondition> conditionResponse =
                goodsServiceFacade.queryGoodsFilterCondition(new ReqGoodsConditionVo());
        ApiResponseCheck.check(conditionResponse);
        GoodsFilterCondition goodsFilterCondition = conditionResponse.getBody();
        // 科目筛选条件
        List<CommonConditionVo> subjects = goodsManager.querySubjectCondition(goodsFilterCondition.getSubjectIds());
        // 查询学科详情
        List<SubjectProductBo> subjectProductList = goodsManager.querySubjectProduct(goodsFilterCondition.getSubjectProductIds());
        Map<Integer, List<SubjectProductBo>> subjectProductBoMap = CollectionCommonUtil.groupByList(
                subjectProductList, "getSubjectId", Integer.class);
        // 将学科筛选条件作为科目筛选条件的子条件
        for (CommonConditionVo subject : subjects) {
            if(subjectProductBoMap.containsKey(subject.getId())) {
                List<CommonConditionVo> subjectProducts = baseMapper.mapAsList(
                        subjectProductBoMap.get(subject.getId()), CommonConditionVo.class);
                subjectProducts.add(0, goodsManager.addAllCondition());
                subject.setChildConditions(subjectProducts);
            }
        }
        return ResultData.successed(subjects);
    }

    /**
     * 获取全部筛选条件
     * @return
     */
    @RequestMapping(value = "/queryCondition", method = RequestMethod.GET)
    public ResultData queryCondition(ReqGoodsConditionVo reqGoodsConditionVo) {
        // 获取教材筛选条件的ID集合
        ApiResponse<GoodsFilterCondition> conditionResponse =
                goodsServiceFacade.queryGoodsFilterCondition(reqGoodsConditionVo);
        ApiResponseCheck.check(conditionResponse);
        GoodsFilterCondition goodsFilterCondition = conditionResponse.getBody();
        // 全部筛选条件
        List<CommonConditionVo> allCondition = new ArrayList<>();
        // 科目筛选条件
        List<CommonConditionVo> subjects = goodsManager.querySubjectCondition(goodsFilterCondition.getSubjectIds());
        allCondition.add(new CommonConditionVo(0,"科目",subjects));
        // 学科筛选条件
        List<CommonConditionVo> subjectProducts = goodsManager.querySubjectProductCondition(goodsFilterCondition.getSubjectProductIds());
        allCondition.add(new CommonConditionVo(1,"学科",subjectProducts));
        // 体系筛选条件
        List<CommonConditionVo> schemes = goodsManager.querySchemeCondition(goodsFilterCondition.getSchemeIds(),subjectProducts);
        allCondition.add(new CommonConditionVo(2,"学科体系",schemes));
        // 学期筛选条件
        List<CommonConditionVo> periods = goodsManager.queryPeriodCondition(goodsFilterCondition.getPeriodIds());
        allCondition.add(new CommonConditionVo(3,"适用学期",periods));
        // 匹配条件
        List<CommonConditionVo> categoty = new ArrayList<>();
        categoty.add(goodsManager.addAllCondition());
        if(CollectionUtils.isNotEmpty(goodsFilterCondition.getBookVersionIds())) {
            List<CommonConditionVo> bookVersions = goodsManager.queryBookVersionCondition(goodsFilterCondition.getBookVersionIds());
            categoty.add(new CommonConditionVo(1, "教材版本",bookVersions));
        }
        if(CollectionUtils.isNotEmpty(goodsFilterCondition.getExamAreaIds())) {
            List<CommonConditionVo> examAreas = goodsManager.queryExamAreaCondition(goodsFilterCondition.getExamAreaIds());
            categoty.add(new CommonConditionVo(2, "考区版本", examAreas));
        }
        allCondition.add(new CommonConditionVo(4,"匹配条件",categoty));
        return ResultData.successed(allCondition);
    }

    /**
     * 根据条件查询教材列表
     * @param reqGoodsConditionVo
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ResultData queryList(ReqGoodsConditionVo reqGoodsConditionVo){
        reqGoodsConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        ApiResponse<Page<GoodsVo>> response = goodsServiceFacade.queryGoodsList(reqGoodsConditionVo);
        ApiResponseCheck.check(response);
        Page<GoodsVo> page = response.getBody();
        dealGoodsVo(Lists.newArrayList(page.getList()));
        return ResultData.successed(page);
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
        bookVersionIds.remove(0);
        examAreaIds.remove(0);
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
