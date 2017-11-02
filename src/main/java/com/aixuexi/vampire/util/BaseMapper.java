package com.aixuexi.vampire.util;

import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.vulcan.vo.CommonConditionVo;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.springframework.stereotype.Component;

/**
 * @author zhouxiong
 * on 2017/3/21 17:14.
 */
@Component
public class BaseMapper extends ConfigurableMapper {

    @Override
    protected void configure(MapperFactory factory) {
        factory.classMap(DictionaryBo.class, CommonConditionVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .fieldAToB("code", "id")
                .register();

    }
}
