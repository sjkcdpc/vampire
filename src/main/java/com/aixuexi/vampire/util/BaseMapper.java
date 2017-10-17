package com.aixuexi.vampire.util;

import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.vulcan.model.LogisticsData;
import com.gaosi.api.vulcan.vo.CommonConditionVo;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author zhouxiong
 *         on 2017/3/21 17:14.
 */
@Component
public class BaseMapper extends ConfigurableMapper {

    @Value("#{${pay_total_time}}")
    private Long payTotal;

    @Override
    protected void configure(MapperFactory factory) {
        factory.classMap(DictionaryBo.class,CommonConditionVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .fieldAToB("code","id")
                .register();
        factory.classMap(GoodsOrder.class,GoodsOrderVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .field("orderDetails", "orderDetailVos")
                .customize(new CustomMapper<GoodsOrder, GoodsOrderVo>(){
                    @Override
                    public void mapAtoB(GoodsOrder goodsOrder, GoodsOrderVo goodsOrderVo, MappingContext context) {
                        //物流信息
                        if(StringUtils.isNotBlank(goodsOrder.getLogistics())) {
                            JSONObject logJson = (JSONObject) JSONObject.parse(goodsOrder.getLogistics());
                            goodsOrderVo.setLogdata(JSONObject.parseArray(logJson.getString("data"), LogisticsData.class));
                        }
                        //待支付剩余时间，单位秒
                        long creatTime = goodsOrder.getCreateOrderTime().getTime();
                        long current = System.currentTimeMillis();
                        long payRemainTime = (payTotal-(current-creatTime))/1000;
                        payRemainTime = payRemainTime>0 ? payRemainTime : 0;
                        goodsOrderVo.setPayRemainTime(payRemainTime);
                    }
                })
                .register();
    }
}
