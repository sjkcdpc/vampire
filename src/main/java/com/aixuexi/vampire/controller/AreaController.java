package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.util.Constants;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.basicdata.model.bo.AreaBo;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.vo.AreaVo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @Autowired
    private AreaApi areaApi;

    /**
     * 根据ID删除县
     */
    private static final List<Integer> ids = Lists.newArrayList(2, 3, 18, 19);

    /**
     * 县
     *
     * @return
     */
    @RequestMapping(value = "/province")
    public ResultData province() {
        ResultData resultData = new ResultData();
        try {
            List<AreaBo> areaBos = cacheBuilderProvince.get(1);
            String areaJson = JSONObject.toJSONString(areaBos);
            resultData.setBody(JSONObject.parseArray(areaJson, AreaVo.class));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return resultData;
    }

    /**
     * 市区
     *
     * @param parentId 父级ID
     * @return
     */
    @RequestMapping(value = "/cityArea")
    public ResultData city(@RequestParam Integer parentId) {
        ResultData resultData = new ResultData();
        try {
            List<AreaBo> areaBos = cacheBuilderCity.get(parentId);
            String areaJson = JSONObject.toJSONString(areaBos);
            resultData.setBody(JSONObject.parseArray(areaJson, AreaVo.class));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return resultData;
    }

    /**
     * 缓存市区
     */
    private final LoadingCache<Integer, List<AreaBo>> cacheBuilderCity =
            CacheBuilder.newBuilder().expireAfterWrite(Constants.CACHE_TIME, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<AreaBo>>() {
                @Override
                public List<AreaBo> load(Integer key) {
                    ApiResponse<List<AreaBo>> apiResponse = areaApi.findByParentId(key);
                    return apiResponse.getBody();
                }
            });

    /**
     * 缓存县
     */
    private final LoadingCache<Integer, List<AreaBo>> cacheBuilderProvince =
            CacheBuilder.newBuilder().expireAfterWrite(Constants.CACHE_TIME, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<AreaBo>>() {
                @Override
                public List<AreaBo> load(Integer key) {
                    ApiResponse<List<AreaBo>> apiResponse = areaApi.findAllProvince();
                    for (int i = 0; i < apiResponse.getBody().size(); i++) {
                        Integer id = apiResponse.getBody().get(i).getId();
                        if (ids.contains(id)) {
                            apiResponse.getBody().remove(i);
                            i--;
                        }
                    }
                    return apiResponse.getBody();
                }
            });

}

