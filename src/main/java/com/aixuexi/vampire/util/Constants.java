package com.aixuexi.vampire.util;

import com.gaosi.api.workorder.constant.FieldType;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoxinzhong on 2017/6/2.
 */
public final class Constants {

    /**
     * 学期映射
     */
    public static Map<Integer, Integer> PERIOD_MAP = new HashMap<>();
    static {
        PERIOD_MAP.put(1, 8);//春 1000
        PERIOD_MAP.put(2, 4);//暑 0100
        PERIOD_MAP.put(3, 2);//秋 0010
        PERIOD_MAP.put(4, 1);//寒 0001
    }

    /**
     * 人才中心模板编码
     */
    public static final String RCZX_TEMPLATE_CODE = "rczx";

    /**
     * 动态工单中特殊字段类型
     */
    public static List<String> SPECIAL_FIELD_TYPE = Lists.newArrayList(FieldType.CHECKBOX.getValue(),
            FieldType.RADIO.getValue(),FieldType.SELECT.getValue());
}
