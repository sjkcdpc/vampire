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
    //机构类型 0 正式 1测试机构 2试用机构 3外研英语机构
    public static final List<Integer> INS_TYPES = Lists.newArrayList(1, 2);

    /**
     * 缓存0.5个小时
     */
    public static final long CACHE_TIME = 1800;

    /**
     * 试用机构
     */
    public static final Integer INSTITUTION_TYPE_TEST_USE = 2;


    public static final String PRE_SHOPPINGCART ="SHOPPINGCART_";
    //double 类型 前端显示的默认值
    public static final String  DEFAULT_DOUBLE_VALUE="0.00";
    //免运费提示
    public static final String FREE_FREIGHT="已满50件，免物流服务费";
}
