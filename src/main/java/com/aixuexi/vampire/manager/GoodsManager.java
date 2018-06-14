package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.vampire.util.BaseMapper;
import com.gaosi.api.basicdata.model.bo.*;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.CommonConditionVo;
import com.google.common.collect.ImmutableCollection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by ruanyanjie on 2018/1/19.
 */
@Service("goodsManager")
public class GoodsManager {
    private final Logger logger = LoggerFactory.getLogger(GoodsManager.class);

    @Resource
    private CacheManager cacheManager;

    @Resource
    private BaseMapper baseMapper;

    // 学期的排序规则
    private Map<String,Integer> periodMap = new HashMap<>();

    // 排序规则初始化（暑，秋，寒，春，上册，下册）
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
     * 查询科目筛选条件
     * @param subjectIds
     * @return
     */
    public List<CommonConditionVo> querySubjectCondition(List<Integer> subjectIds){
        try {
            ImmutableCollection<SubjectBo> subjectBos = cacheManager.getCacheSubject().getAll(subjectIds).values();
            List<SubjectBo> subjectBoList = new ArrayList<>(subjectBos);
            // 按照ID排序
            Collections.sort(subjectBoList, new Comparator<SubjectBo>() {
                @Override
                public int compare(SubjectBo o1, SubjectBo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            // 科目列表
            List<CommonConditionVo> subjects = baseMapper.mapAsList(subjectBoList,CommonConditionVo.class);
            return subjects;
        } catch (Exception e) {
            logger.error("查询科目筛选条件异常 subjectIds : {}",subjectIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"查询科目筛选条件异常");
        }
    }

    /**
     * 获取学科
     * @param subjectProductIds
     * @return
     */
    public List<SubjectProductBo> querySubjectProduct(List<Integer> subjectProductIds){
        try {
            ImmutableCollection<SubjectProductBo> subjectProductBos = cacheManager.getCacheSubjectProduct().getAll(subjectProductIds).values();
            // 学科列表
            List<SubjectProductBo> subjectProducts = new ArrayList<>(subjectProductBos);
            // 学科按orderIndex排序
            Collections.sort(subjectProducts, new Comparator<SubjectProductBo>() {
                @Override
                public int compare(SubjectProductBo o1, SubjectProductBo o2) {
                    return o1.getOrderIndex().compareTo(o2.getOrderIndex());
                }
            });
            return subjectProducts;
        } catch (Exception e) {
            logger.error("查询学科异常 subjectProductIds : {}",subjectProductIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"查询学科异常");
        }
    }

    /**
     * 获取学科筛选条件
     * @param subjectProductIds
     * @return
     */
    public List<CommonConditionVo> querySubjectProductCondition(List<Integer> subjectProductIds){
        try {
            List<SubjectProductBo> subjectProductBoList = querySubjectProduct(subjectProductIds);
            List<CommonConditionVo> subjectProducts = baseMapper.mapAsList(subjectProductBoList,CommonConditionVo.class);
            subjectProducts.add(0, addAllCondition());
            return subjectProducts;
        } catch (Exception e) {
            logger.error("查询学科筛选条件异常 subjectProductIds : {}",subjectProductIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"查询学科筛选条件异常");
        }
    }

    /**
     * 获取体系筛选条件
     * @param schmeIds
     * @return
     */
    public List<CommonConditionVo> querySchemeCondition(List<Integer> schmeIds,List<CommonConditionVo> subjectProducts){
        try {
            ImmutableCollection<SchemeBo> schemeBos = cacheManager.getCacheScheme().getAll(schmeIds).values();
            List<SchemeBo> schemeBoList = new ArrayList<>(schemeBos);
            // 体系按ID排序
            Collections.sort(schemeBoList, new Comparator<SchemeBo>() {
                @Override
                public int compare(SchemeBo o1, SchemeBo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            // 排好序的学科ID
            List<Integer> subjectProductIds = CollectionCommonUtil.getFieldListByObjectList(
                    subjectProducts, "getId", Integer.class);
            // 把体系按照排序好的学科ID分类
            Map<Integer,List<SchemeBo>> schemeBoMap = new HashMap<>();
            for (Integer subjectProductId : subjectProductIds) {
                schemeBoMap.put(subjectProductId,new ArrayList<SchemeBo>());
            }
            for (SchemeBo schemeBo : schemeBoList) {
                if(schemeBoMap.containsKey(schemeBo.getSubjectProductId())){
                    schemeBoMap.get(schemeBo.getSubjectProductId()).add(schemeBo);
                }
            }
            // 排好序的体系集合
            List<SchemeBo> sortedSchemeBos = new ArrayList<>();
            for (Integer subjectProductId : subjectProductIds) {
                if(schemeBoMap.containsKey(subjectProductId)){
                    sortedSchemeBos.addAll(schemeBoMap.get(subjectProductId));
                }
            }
            List<CommonConditionVo> schemes =  baseMapper.mapAsList(sortedSchemeBos,CommonConditionVo.class);
            schemes.add(0, addAllCondition());
            return schemes;
        } catch (Exception e) {
            logger.error("查询体系筛选条件异常 schmeIds : {} , subjectProducts : {}", schmeIds, subjectProducts);
            throw new BusinessException(ExceptionCode.UNKNOWN,"查询体系筛选条件异常");
        }
    }

    /**
     * 获取学期筛选条件
     * @param periodIds
     * @return
     */
    public List<CommonConditionVo> queryPeriodCondition(List<Integer> periodIds){
        try {
            ImmutableCollection<DictionaryBo> dictionaryBos = cacheManager.getCachePeriod().getAll(periodIds).values();
            List<DictionaryBo> dictionaryList = new ArrayList<>(dictionaryBos);
            // 学期按照orderIndex排序
            for (DictionaryBo dictionaryBo : dictionaryList) {
                resetPeriodOrder(dictionaryBo);
            }
            Collections.sort(dictionaryList, new Comparator<DictionaryBo>() {
                @Override
                public int compare(DictionaryBo o1, DictionaryBo o2) {
                    return o1.getOrderIndex().compareTo(o2.getOrderIndex());
                }
            });
            List<CommonConditionVo> periods = baseMapper.mapAsList(dictionaryList,CommonConditionVo.class);
            periods.add(0,addAllCondition());
            return periods;
        } catch (Exception e) {
            logger.error("获取学期筛选条件异常 periodIds : " + periodIds.toString(), e);
            throw new BusinessException(ExceptionCode.UNKNOWN,"获取学期筛选条件异常");
        }
    }

    /**
     * 获取教材版本筛选条件
     * @param bookVersionIds
     * @return
     */
    public List<CommonConditionVo> queryBookVersionCondition(List<Integer> bookVersionIds){
        try {
            ImmutableCollection<BookVersionBo> bookVersionBos = cacheManager.getCacheBookVersion().getAll(bookVersionIds).values();
            List<BookVersionBo> bookVersionBoList = new ArrayList<>(bookVersionBos);
            // 教材版本按照orderIndex排序
            Collections.sort(bookVersionBoList, new Comparator<BookVersionBo>() {
                @Override
                public int compare(BookVersionBo o1, BookVersionBo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            List<CommonConditionVo> bookVersions = baseMapper.mapAsList(bookVersionBoList,CommonConditionVo.class);
            bookVersions.add(0,addAllCondition());
            return bookVersions;
        } catch (ExecutionException e) {
            logger.error("获取教材版本筛选条件异常 bookVersionIds : {} ",bookVersionIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"获取教材版本筛选条件异常");
        }
    }

    /**
     * 获取考区版本筛选条件
     * @param examAreaIds
     * @return
     */
    public List<CommonConditionVo> queryExamAreaCondition(List<Integer> examAreaIds){
        try {
            ImmutableCollection<ExamAreaBo> examAreaBos = cacheManager.getCacheExamArea().getAll(examAreaIds).values();
            List<ExamAreaBo> examAreaBoList = new ArrayList<>(examAreaBos);
            // 考区版本按照orderIndex排序
            Collections.sort(examAreaBoList, new Comparator<ExamAreaBo>() {
                @Override
                public int compare(ExamAreaBo o1, ExamAreaBo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            List<CommonConditionVo> examAreas = baseMapper.mapAsList(examAreaBoList,CommonConditionVo.class);
            examAreas.add(0,addAllCondition());
            return examAreas;
        } catch (Exception e) {
            logger.error("获取考区版本筛选条件异常 examAreaIds : {} ",examAreaIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"获取考区版本筛选条件异常");
        }
    }

    /**
     * 添加全部条件
     * @return
     */
    public CommonConditionVo addAllCondition(){
        CommonConditionVo commonConditionVo = new CommonConditionVo();
        commonConditionVo.setId(0);
        commonConditionVo.setName("全部");
        commonConditionVo.setCode(StringUtils.EMPTY);
        return commonConditionVo;
    }

    /**
     * 重置学期排序
     * @param db
     */
    public void resetPeriodOrder(DictionaryBo db) {
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
}
