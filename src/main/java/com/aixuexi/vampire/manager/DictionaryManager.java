package com.aixuexi.vampire.manager;

import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoxinzhong on 2017/6/2.
 */
@Service("dictionaryManager")
public class DictionaryManager {

    private static Logger logger = LoggerFactory.getLogger(DictionaryManager.class);

    @Resource
    private CacheManager cacheManager;

    /**
     * 根据类型和code查询类别名称
     * @param type
     * @param code
     * @return
     */
    public String getCategory(String type, String code){
        List<DictionaryBo> dictionaryBos = selectDictByType(type);
        if (!CollectionUtils.isNotEmpty(dictionaryBos)) {
            return null;
        }

        for (DictionaryBo dictionaryBo : dictionaryBos) {
            if (dictionaryBo.getCode().equals(code)){
                return dictionaryBo.getName();
            }
        }

        return null;
    }

    /**
     * 返回字典key,value
     *
     * @param type
     * @return
     */
    public Map<String, String> selectDictMapByType(final String type) {
        Map<String, String> retMap = Maps.newHashMap();
        List<DictionaryBo> dictionaryBos = selectDictByType(type);
        if (!CollectionUtils.isNotEmpty(dictionaryBos)) {
            return retMap;
        }

        for (DictionaryBo dictionaryBo : dictionaryBos) {
            String code = dictionaryBo.getCode();
            String name = dictionaryBo.getName();
            retMap.put(code, name);
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
            return cacheManager.getCacheBuilderDict().get(type);
        } catch (Exception e) {
            logger.error("selectDictByType type : {} - catch exception ", type, e);
        }
        return null;
    }



}
