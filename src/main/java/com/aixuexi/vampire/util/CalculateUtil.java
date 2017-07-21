package com.aixuexi.vampire.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by Administrator on 2017/7/21.
 */
public class CalculateUtil {
    /**
     * 加法
     *
     * @param d1
     * @param d2
     * @return
     */
    public static double add(Double d1, Double d2) {
        BigDecimal bigDecimal1 = new BigDecimal(d1 == null ? 0 : d1);
        BigDecimal bigDecimal2 = new BigDecimal(d2 == null ? 0 : d2);
        return bigDecimal1.add(bigDecimal2).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 乘法
     *
     * @param d1
     * @param d2
     * @return
     */
    public static double mul(Double d1, Double d2) {
        BigDecimal bigDecimal1 = new BigDecimal(d1 == null ? 0 : d1);
        BigDecimal bigDecimal2 = new BigDecimal(d2 == null ? 0 : d2);
        return bigDecimal1.multiply(bigDecimal2).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
