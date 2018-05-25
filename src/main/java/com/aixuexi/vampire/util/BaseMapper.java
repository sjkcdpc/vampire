package com.aixuexi.vampire.util;

import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.SubjectBo;
import com.gaosi.api.revolver.bean.common.LogisticsTrackDetail;
import com.gaosi.api.revolver.vo.OrderDetailVo;
import com.gaosi.api.vulcan.vo.CommonConditionVo;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.xmen.model.TalentOperatorRecords;
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
                .customize(new CustomMapper<DictionaryBo, CommonConditionVo>() {
                    @Override
                    public void mapAtoB(DictionaryBo dictionaryBo, CommonConditionVo commonConditionVo, MappingContext context) {
                        commonConditionVo.setId(Integer.parseInt(dictionaryBo.getCode()));
                    }
                })
                .register();
        factory.classMap(SubjectBo.class, CommonConditionVo.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .customize(new CustomMapper<SubjectBo, CommonConditionVo>() {
                    @Override
                    public void mapAtoB(SubjectBo subjectBo, CommonConditionVo commonConditionVo, MappingContext context) {
                        commonConditionVo.setName(subjectBo.getBrandName());
                    }
                })
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
        factory.classMap(TalentOperatorRecords.class, LogisticsTrackDetail.class)
                .mapNulls(true).mapNullsInReverse(true)
                .byDefault()
                .customize(new CustomMapper<TalentOperatorRecords, LogisticsTrackDetail>() {
                    @Override
                    public void mapAtoB(TalentOperatorRecords talentOperatorRecords, LogisticsTrackDetail logisticsData, MappingContext context) {
                        logisticsData.setTime(talentOperatorRecords.getCreateTime());
                        logisticsData.setContext(talentOperatorRecords.getDescription());
                    }
                })
                .register();
    }
}
