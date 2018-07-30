package com.aixuexi.vampire.util;

import com.gaosi.api.workorder.constant.FieldType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


/**
 * Created by gaoxinzhong on 2017/6/2.
 */
public final class Constants {

    /**
     * 学期映射 春 1000  暑 0100  秋 0010  寒 0001
     */
    public static final ImmutableMap <Integer, Integer> PERIOD_MAP = ImmutableMap.of(1, 8,2, 4,3, 2,4, 1);

    /**
     * 人才中心模板编码
     */
    public static final String RCZX_TEMPLATE_CODE = "rczx";

    /**
     * 动态工单中特殊字段类型
     */
    public static final ImmutableList<String> SPECIAL_FIELD_TYPE = ImmutableList.of(FieldType.CHECKBOX.getValue(),FieldType.RADIO.getValue(),FieldType.SELECT.getValue());
}
