package com.aixuexi.vampire.manager;

import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.util.BaseMapper;
import com.gaosi.api.basicdata.*;
import com.gaosi.api.basicdata.model.bo.*;
import com.gaosi.api.basicdata.model.vo.DistrictVO;
import com.gaosi.api.common.basedao.PageParam;
import com.gaosi.api.common.basedao.SortTypeEnum;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.constant.GoodsConstant;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.AreaVo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruanyanjie on 2018/6/1.
 */
@Service("cacheManager")
public class CacheManager {
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
    private SubjectApi subjectApi;

    @Resource
    private DistrictApi districtApi;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 缓存市区
     */
    private LoadingCache<Integer, List<AreaVo>> cacheBuilderCity =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<AreaVo>>() {
                @Override
                public List<AreaVo> load(Integer key) {
                    DistrictVO districtVO = new DistrictVO();//查询条件
                    districtVO.setParentId(key);//父ID
                    PageParam pageParam = new PageParam();
                    pageParam.setPageSize(10000);
                    pageParam.setSortOrder(SortTypeEnum.ID_ASC.getSortId());

                    ApiResponse<Page<DistrictVO>> apiResponse = districtApi.getPageByCondition(districtVO, pageParam);
                    Page<DistrictVO> districtVOPage = apiResponse.getBody();
                    List<DistrictVO> districtVOs = districtVOPage.getList();
                    return baseMapper.mapAsList(districtVOs, AreaVo.class);
                }
            });

    /**
     * 缓存省
     */
    private LoadingCache<Integer, List<AreaVo>> cacheBuilderProvince =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<AreaVo>>() {
                @Override
                public List<AreaVo> load(Integer key) {
                    DistrictVO districtVO = new DistrictVO();
                    districtVO.setLevel(1);//省
                    PageParam pageParam = new PageParam();
                    pageParam.setPageSize(100);
                    pageParam.setSortOrder(SortTypeEnum.ID_ASC.getSortId());

                    ApiResponse<Page<DistrictVO>> apiResponse = districtApi.getPageByCondition(districtVO, pageParam);
                    Page<DistrictVO> districtVOPage = apiResponse.getBody();
                    List<DistrictVO> districtVOs = districtVOPage.getList();
                    return baseMapper.mapAsList(districtVOs, AreaVo.class);
                }
            });

    /**
     * 缓存科目
     */
    private LoadingCache<Integer, SubjectBo> cacheSubject =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(
                    new CacheLoader<Integer, SubjectBo>() {
                        @Override
                        public SubjectBo load(Integer subjectId) {
                            ApiResponse<SubjectBo> subjectResponse = subjectApi.getById(subjectId);
                            SubjectBo subjectBo = subjectResponse.getBody();
                            return subjectBo;
                        }

                        @Override
                        public Map<Integer, SubjectBo> loadAll(Iterable<? extends Integer> subjectIds) {
                            // 查询科目详情
                            ApiResponse<List<SubjectBo>> subjectResponse = subjectApi.getByIds(Lists.newArrayList(subjectIds));
                            List<SubjectBo> subjectBos = subjectResponse.getBody();
                            return CollectionCommonUtil.toMapByList(subjectBos, "getId", Integer.class);
                        }
                    });

    /**
     * 缓存学科
     */
    private LoadingCache<Integer, SubjectProductBo> cacheSubjectProduct =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(
                    new CacheLoader<Integer, SubjectProductBo>() {
                        @Override
                        public SubjectProductBo load(Integer subjectProductId) {
                            ApiResponse<SubjectProductBo> subjectProductResponse = subjectProductApi.getById(subjectProductId);
                            SubjectProductBo subjectProductBo = subjectProductResponse.getBody();
                            return subjectProductBo;
                        }

                        @Override
                        public Map<Integer, SubjectProductBo> loadAll(Iterable<? extends Integer> subjectProductIds) {
                            // 查询学科详情
                            List<SubjectProductBo> subjectProductBos = subjectProductApi.findSubjectProductList(Lists.newArrayList(subjectProductIds));
                            return CollectionCommonUtil.toMapByList(subjectProductBos, "getId", Integer.class);
                        }
                    });

    /**
     * 缓存体系
     */
    private LoadingCache<Integer, SchemeBo> cacheScheme =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(
                    new CacheLoader<Integer, SchemeBo>() {
                        @Override
                        public SchemeBo load(Integer schmeId) {
                            ApiResponse<SchemeBo> schmeResponse = schemeApi.getById(schmeId);
                            SchemeBo schemeBo = schmeResponse.getBody();
                            return schemeBo;
                        }

                        @Override
                        public Map<Integer, SchemeBo> loadAll(Iterable<? extends Integer> schmeIds) {
                            // 查询体系详情
                            ApiResponse<List<SchemeBo>> schemeResponse = schemeApi.getByIds(Lists.newArrayList(schmeIds));
                            List<SchemeBo> schemeBos = schemeResponse.getBody();
                            return CollectionCommonUtil.toMapByList(schemeBos, "getId", Integer.class);
                        }
                    });

    /**
     * 缓存学期
     */
    private LoadingCache<Integer, DictionaryBo> cachePeriod =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(
                    new CacheLoader<Integer, DictionaryBo>() {
                        @Override
                        public DictionaryBo load(Integer periodId) {
                            ApiResponse<List<DictionaryBo>> periodResponse = dictionaryApi.findGoodsPeriodByCode(Lists.newArrayList(periodId));
                            List<DictionaryBo> dictionaryBos = periodResponse.getBody();
                            return dictionaryBos.get(0);
                        }

                    });

    /**
     * 缓存教材版本
     */
    private LoadingCache<Integer, BookVersionBo> cacheBookVersion =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(
                    new CacheLoader<Integer, BookVersionBo>() {
                        @Override
                        public BookVersionBo load(Integer bookVersionId) {
                            ApiResponse<BookVersionBo> bookVersionResponse = bookVersionApi.getById(bookVersionId);
                            BookVersionBo bookVersionBo = bookVersionResponse.getBody();
                            return bookVersionBo;
                        }

                        @Override
                        public Map<Integer, BookVersionBo> loadAll(Iterable<? extends Integer> bookVersionIds) {
                            ApiResponse<List<BookVersionBo>> bookVersionResponse = bookVersionApi.findByBookVersionIds(Lists.newArrayList(bookVersionIds));
                            List<BookVersionBo> bookVersionBos = bookVersionResponse.getBody();
                            return CollectionCommonUtil.toMapByList(bookVersionBos, "getId", Integer.class);
                        }
                    });

    /**
     * 缓存考区版本
     */
    private LoadingCache<Integer, ExamAreaBo> cacheExamArea =
            CacheBuilder.newBuilder().expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS).build(
                    new CacheLoader<Integer, ExamAreaBo>() {
                        @Override
                        public ExamAreaBo load(Integer examAreaId) {
                            ApiResponse<ExamAreaBo> examAreaResponse = examAreaApi.getById(examAreaId);
                            ExamAreaBo examAreaBo = examAreaResponse.getBody();
                            return examAreaBo;
                        }

                        @Override
                        public Map<Integer, ExamAreaBo> loadAll(Iterable<? extends Integer> examAreaIds) {
                            List<ExamAreaBo> examAreaBos = examAreaApi.queryByIds(Lists.newArrayList(examAreaIds));
                            return CollectionCommonUtil.toMapByList(examAreaBos, "getId", Integer.class);
                        }
                    });

    /**
     * 缓存字典
     */
    private LoadingCache<String, List<DictionaryBo>> cacheBuilderDict = CacheBuilder.newBuilder()
            .expireAfterWrite(GoodsConstant.BASIC_DATA_CACHE_TIME, TimeUnit.SECONDS)
            .build(new CacheLoader<String, List<DictionaryBo>>() {
                @Override
                public List<DictionaryBo> load(String key) throws Exception {
                    ApiResponse<List<DictionaryBo>> apiResponse = dictionaryApi.listAllByStatus(1, key);
                    return apiResponse.getBody();
                }
            });

    public LoadingCache<Integer, List<AreaVo>> getCacheBuilderCity() {
        return cacheBuilderCity;
    }

    public LoadingCache<Integer, List<AreaVo>> getCacheBuilderProvince() {
        return cacheBuilderProvince;
    }

    public LoadingCache<Integer, SubjectBo> getCacheSubject() {
        return cacheSubject;
    }

    public LoadingCache<Integer, SubjectProductBo> getCacheSubjectProduct() {
        return cacheSubjectProduct;
    }

    public LoadingCache<Integer, SchemeBo> getCacheScheme() {
        return cacheScheme;
    }

    public LoadingCache<Integer, DictionaryBo> getCachePeriod() {
        return cachePeriod;
    }

    public LoadingCache<Integer, BookVersionBo> getCacheBookVersion() {
        return cacheBookVersion;
    }

    public LoadingCache<Integer, ExamAreaBo> getCacheExamArea() {
        return cacheExamArea;
    }

    public LoadingCache<String, List<DictionaryBo>> getCacheBuilderDict() {
        return cacheBuilderDict;
    }

    /**
     * 清除所有缓存
     */
    public void invalidateAll(){
        cacheBuilderCity.invalidateAll();
        cacheBuilderProvince.invalidateAll();
        cacheSubject.invalidateAll();
        cacheSubjectProduct.invalidateAll();
        cacheScheme.invalidateAll();
        cachePeriod.invalidateAll();
        cacheBookVersion.invalidateAll();
        cacheExamArea.invalidateAll();
        cacheBuilderDict.invalidateAll();
    }
}
