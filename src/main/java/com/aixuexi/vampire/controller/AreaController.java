package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.Constants;
import com.gaosi.api.basicdata.DistrictApi;
import com.gaosi.api.basicdata.model.vo.DistrictVO;
import com.gaosi.api.common.basedao.PageParam;
import com.gaosi.api.common.basedao.SortTypeEnum;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.vo.AreaVo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 地区
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/area")
public class AreaController {

    private final Logger logger = LoggerFactory.getLogger(AreaController.class);

    @Resource
    private DistrictApi districtApi;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 省
     *
     * @return
     */
    @RequestMapping(value = "/province",method = RequestMethod.GET)
    public ResultData province() {
        ResultData resultData = new ResultData();
        try {
            List<AreaVo> areaVos = cacheBuilderProvince.get(1);
            resultData.setBody(areaVos);
        } catch (ExecutionException e) {
            logger.error("获取省份信息失败", e);
            resultData = ResultData.failed("获取省份信息失败");
        }
        return resultData;
    }

    /**
     * 市区
     *
     * @param parentId 父级ID
     * @return
     */
    @RequestMapping(value = "/cityArea",method = RequestMethod.GET)
    public ResultData city(@RequestParam Integer parentId) {
        ResultData resultData = new ResultData();
        try {
            List<AreaVo> areaVos = cacheBuilderCity.get(parentId);
            resultData.setBody(areaVos);
        } catch (ExecutionException e) {
            logger.error("获取市区信息失败", e);
            resultData = ResultData.failed("获取市区信息失败");
        }
        return resultData;
    }

    /**
     * 缓存市区
     */
    private final LoadingCache<Integer, List<AreaVo>> cacheBuilderCity =
            CacheBuilder.newBuilder().expireAfterWrite(Constants.CACHE_TIME, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<AreaVo>>() {
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
    private final LoadingCache<Integer, List<AreaVo>> cacheBuilderProvince =
            CacheBuilder.newBuilder().expireAfterWrite(Constants.CACHE_TIME, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<AreaVo>>() {
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
}

