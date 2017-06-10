package com.aixuexi.vampire.util;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by gaoxinzhong on 2017/6/2.
 */
public final class Constants {

    /**
     * 快递公司字典TYPE
     */
    public static final String DELIVERY_COMPANY_DICT_TYPE = "DELIVERY_COMPANY";

    /**
     * 库存销售模式
     */
    public static final String MALL_SALES_MODEL = "mall_sales_mode";

    public static final String EXPRESS = "shunfeng,shentong";

    public static final String EXPRESS_DBWL = "debangwuliu";

    public static final String EXPRESS_SHENTONG = "shentong";

    public static final String EXPRESS_SHUNFENG = "shunfeng";

    public static final List<Integer> INS_IDS = Lists.newArrayList(25, 26);

    /**
     * 缓存0.5个小时
     */
    public static final long CACHE_TIME = 1800;

}
