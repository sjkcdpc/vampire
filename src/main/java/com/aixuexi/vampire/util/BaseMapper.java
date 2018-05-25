package com.aixuexi.vampire.util;

import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.SubjectBo;
import com.gaosi.api.revolver.vo.OrderDetailVo;
import com.gaosi.api.vulcan.vo.CommonConditionVo;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
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
        factory.classMap(SubjectBo.class, CommonConditionVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .fieldAToB("brandName", "name")
                .register();
        factory.classMap(ConfirmGoodsVo.class,OrderDetailVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .customize(new CustomMapper<ConfirmGoodsVo, OrderDetailVo>(){
                    @Override
                    public void mapAtoB(ConfirmGoodsVo confirmGoodsVo, OrderDetailVo orderDetailVo, MappingContext context) {
                        orderDetailVo.setName(confirmGoodsVo.getGoodsName());
                        orderDetailVo.setGoodTypeId(confirmGoodsVo.getGoodsTypeId());
                    }
                })
                .register();
    }
}
