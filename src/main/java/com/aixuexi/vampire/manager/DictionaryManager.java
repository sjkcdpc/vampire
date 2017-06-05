package com.aixuexi.vampire.manager;

import com.gaosi.api.basicdata.DictionaryApi;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
        ApiResponse<List<DictionaryBo>> apiResponse = dictionaryApi.listAllByStatus(1, type);
        if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE) {
            return apiResponse.getBody();
        }
        return null;
    }

}
