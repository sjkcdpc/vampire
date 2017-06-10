package com.aixuexi.vampire.manager;

import com.gaosi.api.basicdata.DictionaryApi;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by gaoxinzhong on 2017/6/2.
 */
@Service("dictionaryManager")
public class DictionaryManager {

    @Autowired
    private DictionaryApi dictionaryApi;

    /**
     * 返回字典key,value
     *
     * @param type
     * @return
     */
    public Map<String, String> selectDictMapByType(final String type) {
        Map<String, String> retMap = Maps.newHashMap();
        List<DictionaryBo> dictionaryBos = selectDictByType(type);
        if (CollectionUtils.isNotEmpty(dictionaryBos)) {
            for (DictionaryBo dictionaryBo : dictionaryBos) {
                retMap.put(dictionaryBo.getCode(), dictionaryBo.getName());
            }
        }
        return retMap;
    }

    /**
     * 返回字典list对象
     *
     * @param type
     * @return
     */
    public List<DictionaryBo> selectDictByType(final String type) {
        try {
            return cacheBuilderDict.get(type);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 缓存字典
     */
    private final LoadingCache<String, List<DictionaryBo>> cacheBuilderDict = CacheBuilder.newBuilder().build(new CacheLoader<String, List<DictionaryBo>>() {
        @Override
        public List<DictionaryBo> load(String key) throws Exception {
            ApiResponse<List<DictionaryBo>> apiResponse = dictionaryApi.listAllByStatus(1, key);
            if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
                return apiResponse.getBody();
            }
            return null;
        }
    });

}
