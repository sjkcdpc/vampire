package com.aixuexi.vampire.test;


import com.gaosi.api.revolver.vo.GoodsOrderVo;
import org.junit.Test;

import java.text.DecimalFormat;

/**
 * Created by Administrator on 2017/7/11.
 */
public class OtherTest {
    @Test
    public void fun1(){
        DecimalFormat df1 = new DecimalFormat("0.00");
        GoodsOrderVo goodsOrderVo = new GoodsOrderVo();
        goodsOrderVo.setFreight(100D);
        goodsOrderVo.setFreightDis(df1.format(goodsOrderVo.getFreight()));
        System.out.println(goodsOrderVo.getFreightDis());
    }
}
