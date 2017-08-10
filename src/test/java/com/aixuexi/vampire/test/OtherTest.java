package com.aixuexi.vampire.test;


import com.aixuexi.vampire.util.CustomObjectMapper;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import org.junit.Test;

import java.io.StringWriter;
import java.text.DecimalFormat;

/**
 * Created by Administrator on 2017/7/11.
 */
public class OtherTest {
    @Test
    public void fun1() throws Exception{

        CustomObjectMapper customObjectMapper=new CustomObjectMapper();
        GoodsOrderVo gov = new GoodsOrderVo();
        gov.setConsumeAmount(null);

        //ConfirmOrderVo vo=new ConfirmOrderVo();
        StringWriter sw = new StringWriter();
        customObjectMapper.writeValue(sw, gov);
        ;
//        Double d1 = new Double("12.444");
//        Integer i1 = new Integer("1");
        System.out.println( sw.getBuffer().toString());

//        DecimalFormat df1 = new DecimalFormat("0.00");
//        GoodsOrderVo goodsOrderVo = new GoodsOrderVo();
//        goodsOrderVo.setFreight(100D);
//        goodsOrderVo.setFreightDis(df1.format(goodsOrderVo.getFreight()));
//        System.out.println(goodsOrderVo.getFreightDis());
    }
}
