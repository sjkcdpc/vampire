package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.BasicDataManager;
import com.aixuexi.vampire.manager.GoodsManager;
import com.aixuexi.vampire.manager.ItemOrderManager;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.model.bo.BookVersionBo;
import com.gaosi.api.basicdata.model.bo.ExamAreaBo;
import com.gaosi.api.basicdata.model.bo.SchemeBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.InvServiceFacade;
import com.gaosi.api.revolver.facade.OrderServiceFacade;
import com.gaosi.api.revolver.model.GoodsInventory;
import com.gaosi.api.revolver.vo.MallItemSalesNumVo;
import com.gaosi.api.vulcan.constant.GoodsExtConstant;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.model.Goods;
import com.gaosi.api.vulcan.model.GoodsFilterCondition;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.vo.*;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zhaowenlei on 17/5/22.
 */
@RestController
@RequestMapping(value = "/goods")
public class GoodsController {

    @Resource
    private GoodsServiceFacade goodsServiceFacade;

    @Resource
    private BaseMapper baseMapper;

    @Resource
    private InvServiceFacade invServiceFacade;

    @Resource(name = "goodsManager")
    private GoodsManager goodsManager;

    @Resource
    private ItemOrderManager itemOrderManager;
    @Resource
    private OrderServiceFacade orderServiceFacade;

    @Resource
    private BasicDataManager basicDataManager;

    /**
     * 通过商品名模糊查找商品
     *
     * @param goodName
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/getByGoodsName",method = RequestMethod.GET)
    public ResultData queryByGoodName(@RequestParam String goodName, @RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        ReqGoodsConditionVo conditionVo = new ReqGoodsConditionVo();
        conditionVo.setInstitutionId(UserHandleUtil.getInsId());
        conditionVo.setPageNum(pageNum);
        conditionVo.setPageSize(pageSize);
        conditionVo.setGoodsName(goodName);
        ApiResponse<Page<GoodsVo>> response = goodsServiceFacade.queryGoodsList(conditionVo);
        Page<GoodsVo> page = response.getBody();
        loadGoodsSalesNum(page.getList());
        return ResultData.successed(page);
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
        GoodsFilterCondition goodsFilterCondition = conditionResponse.getBody();
        // 科目筛选条件
        List<CommonConditionVo> subjects = goodsManager.querySubjectCondition(goodsFilterCondition.getSubjectIds());
        // 查询学科详情
        List<SubjectProductBo> subjectProductList = goodsManager.querySubjectProduct(goodsFilterCondition.getSubjectProductIds());
        Map<Integer, List<SubjectProductBo>> subjectProductBoMap = subjectProductList.stream().collect(Collectors.groupingBy(SubjectProductBo::getSubjectId));
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
        Page<GoodsVo> page = response.getBody();
        loadGoodsSalesNum(page.getList());
        return ResultData.successed(page);
    }

    /**
     * 商品详情
     *
     * @param mallItemId
     * @return
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public ResultData queryGoodsDetail(@RequestParam(required = false) Integer mallItemId,@RequestParam(required = false) Integer goodsId) {
        if (mallItemId == null && goodsId == null) {
            return ResultData.failed("参数错误");
        }
        // 兼容DIY教材生成时从消息通知处跳转教材详情
        if (mallItemId == null) {
            ApiResponse<Goods> goodsResponse = goodsServiceFacade.queryGoodsById(goodsId);
            Goods goods = goodsResponse.getBody();
            if (goods == null) {
                return ResultData.failed("商品不存在");
            }
            mallItemId = goods.getMallItemId();
        }
        Integer insId = UserHandleUtil.getInsId();
        ApiResponse<GoodsVo> response = goodsServiceFacade.queryGoodsDetail(mallItemId, insId);
        GoodsVo goodsVo = response.getBody();
        List<SchemeBo> schemeBos = goodsManager.queryScheme(Lists.newArrayList(goodsVo.getScheme()));
        SchemeBo schemeBo = schemeBos.get(0);
        goodsVo.setSchemeStr(schemeBo.getName());
        dealGoodsVo(Lists.newArrayList(goodsVo));
        return ResultData.successed(response.getBody());
    }

    /**
     * 拼接关联商品类型的名称
     * @param relationGoodsVo
     * @param relationGoodsTypes
     */
    private void combineRelationName(RelationGoodsVo relationGoodsVo, List<GoodsType> relationGoodsTypes){
        // 将"+"形式的字符串拼接替换为stringbuilder
        String preRelationName = relationGoodsVo.getRelationName();
        StringBuilder relationNameBuilder = new StringBuilder();
        relationNameBuilder.append(preRelationName);
        relationNameBuilder.append("(");
        int i = 0;
        for (GoodsType goodsType: relationGoodsTypes) {
            String skuName = goodsType.getName();
            if (i != 0) {
                relationNameBuilder.append(",");
            }
            relationNameBuilder.append(skuName);
            i++;
        }
        relationNameBuilder.append(")");
        relationGoodsVo.setRelationName(relationNameBuilder.toString());
    }

    /**
     * 处理GoodsVo
     * @param goodsVoList
     */
    private void dealGoodsVo(List<GoodsVo> goodsVoList){
        loadRelationName(goodsVoList);
        loadGoodsInventory(goodsVoList);
        loadGoodsSalesNum(goodsVoList);
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
            if(CollectionUtils.isNotEmpty(relationGoods)) {
                bookVersionIds.addAll(relationGoods.stream().map(RelationGoodsVo::getBookVersion).collect(Collectors.toSet()));
                examAreaIds.addAll(relationGoods.stream().map(RelationGoodsVo::getExamAreaId).collect(Collectors.toSet()));
            }
        }
        //排除非教材版本ID和非考区ID
        bookVersionIds.remove(0);
        examAreaIds.remove(0);
        //获取教材版本和考区的信息
        Map<Integer, ExamAreaBo> examAreaMap = basicDataManager.getExamAreaByIds(new ArrayList<>(examAreaIds));
        Map<Integer, BookVersionBo> bookVersionMap = basicDataManager.getBookVersionByIds(new ArrayList<>(bookVersionIds));
        //获取关联商品名称
        for (GoodsVo goodsVo : list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relationGoodsVo : relationGoods) {
                List<GoodsType> relationGoodsTypes = relationGoodsVo.getRelationGoodsType();
                if (bookVersionMap.containsKey(relationGoodsVo.getBookVersion())) {
                    BookVersionBo bookVersion = bookVersionMap.get(relationGoodsVo.getBookVersion());
                    relationGoodsVo.setRelationName(bookVersion.getName());
                }
                if (examAreaMap.containsKey(relationGoodsVo.getExamAreaId())) {
                    ExamAreaBo examArea = examAreaMap.get(relationGoodsVo.getExamAreaId());
                    relationGoodsVo.setRelationName(examArea.getName());
                }
                if (StringUtils.isNotBlank(relationGoodsVo.getRelationName()) && CollectionUtils.isNotEmpty(relationGoodsTypes)) {
                    combineRelationName(relationGoodsVo, relationGoodsTypes);
                }
            }
        }
    }

    /**
     * 补全商品库存信息
     * @param goodsVoList
     */
    private void loadGoodsInventory(List<GoodsVo> goodsVoList) {
        Set<String> barCodeList = new HashSet<>();
        for (GoodsVo goodsVo : goodsVoList) {
            List<GoodsTypeVo> goodsTypeVos = goodsVo.getGoodsGrades();
            if (CollectionUtils.isNotEmpty(goodsTypeVos)) {
                barCodeList.addAll(goodsTypeVos.stream().map(GoodsTypeVo::getBarCode).collect(Collectors.toSet()));
            }
        }
        if (CollectionUtils.isNotEmpty(barCodeList)) {
            // 查询单个仓库的最大库存量
            ApiResponse<Map<String, Integer>> apiResponse = invServiceFacade.queryMaxInventory(new ArrayList<>(barCodeList));
            Map<String, Integer> invMap = apiResponse.getBody();
            // 查询所有仓库的总计库存量
            ApiResponse<Map<String, GoodsInventory>> totalInventoryResponse = orderServiceFacade.queryTotalInventory(new ArrayList<>(barCodeList));
            Map<String, GoodsInventory> totalInvMap = totalInventoryResponse.getBody();
            for (GoodsVo goodsVo : goodsVoList) {
                List<GoodsTypeVo> goodsTypeVos = goodsVo.getGoodsGrades();
                if (CollectionUtils.isNotEmpty(goodsTypeVos)) {
                    for (GoodsTypeVo goodsTypeVo : goodsTypeVos) {
                        String barCode = goodsTypeVo.getBarCode();
                        //sku对应的单个仓库的最大库存量
                        goodsTypeVo.setGoodsNum(invMap.get(barCode));
                        //库存数量少于预警库存时，报警
                        GoodsInventory goodsInventory = totalInvMap.get(barCode);
                        Integer inventoryNum = goodsInventory.getGoodsNum();
                        goodsTypeVo.setInventoryNum(inventoryNum);
                        if (goodsVo.getCustomized() == GoodsExtConstant.Customized.COMMON.getValue()
                                && inventoryNum <= goodsTypeVo.getInventory()) {
                            goodsTypeVo.setArrivalMsg(getArrivalMsg(goodsTypeVo.getArrivalTime()));
                        } else {
                            goodsTypeVo.setArrivalMsg("");
                        }
                    }
                }
            }
        }
    }

    /**
     * 根据时间获取缺货通知信息
     *
     * @param arrivalTime 到货时间
     * @return 到货通知信息
     */
    private String getArrivalMsg(Integer arrivalTime) {
        Date date = new Date();
        SimpleDateFormat formater = new SimpleDateFormat("yyyy年MM月dd日");
        long millisecondsPerDay = 24L * 3600 * 1000;
        String result = "预计" + formater.format(new Date(date.getTime() + arrivalTime * millisecondsPerDay)) + "到货";
        String target = new SimpleDateFormat("yyyy年").format(date);
        return result.replace(target, "");
    }

    /**
     * 补全商品销量
     * @param goodsVos
     */
    private void loadGoodsSalesNum(List<GoodsVo> goodsVos) {
        if(CollectionUtils.isNotEmpty(goodsVos)) {
            List<Integer> mallItemIds = goodsVos.stream().map(GoodsVo::getMallItemId).collect(Collectors.toList());
            Map<Integer, MallItemSalesNumVo> salesNumVoMap = itemOrderManager.querySalesNum(mallItemIds);
            for (GoodsVo goodsVo : goodsVos) {
                goodsVo.setSalesNum(salesNumVoMap.get(goodsVo.getMallItemId()).getNum());
            }
        }
    }
}
