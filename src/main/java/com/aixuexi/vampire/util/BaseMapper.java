package com.aixuexi.vampire.util;

import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author zhouxiong
 *         on 2017/3/21 17:14.
 */
@Component
public class BaseMapper extends ConfigurableMapper {
    @Override
    protected void configure(MapperFactory factory) {
        factory.classMap(GoodsOrder.class,GoodsOrderVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .customize(new CustomMapper<GoodsOrder, GoodsOrderVo>() {
                    @Override
                    public void mapAtoB(GoodsOrder goodsOrder, GoodsOrderVo goodsOrderVo, MappingContext context) {
                        Date createOrderTime = goodsOrder.getCreateOrderTime();
                        String createOrderTimeStr = new DateTime(createOrderTime).toString("yyyy-MM-dd HH:mm:ss");
                        goodsOrderVo.setCreateOrderTime(createOrderTimeStr);
                    }
                })
                .register();
    }
}
