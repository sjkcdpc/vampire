package com.aixuexi.vampire.util;

import com.gaosi.api.revolver.constant.ExpressConstant;
import com.gaosi.api.vulcan.vo.ConfirmExpressVo;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoxinzhong on 2017/6/2.
 */
public final class Constants {

    /**
     * 缓存0.5个小时
     */
    public static final long CACHE_TIME = 1800;

    //订单详情中商品名称拼接的分隔符
    public static final String ORDERDETAIL_NAME_DIV ="-";

    /**
     * 学期映射
     */
    public final static Map<Integer, Integer> PERIOD_MAP = new HashMap<>();
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
}
