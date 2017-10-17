package com.aixuexi.vampire.test;


import com.aixuexi.vampire.util.CalculateUtil;
import com.aixuexi.vampire.util.CustomObjectMapper;
import com.gaosi.api.revolver.model.GoodsOrder;
import com.gaosi.api.revolver.util.ExpressCodeUtil;
import com.gaosi.api.revolver.vo.GoodsOrderVo;
import com.gaosi.api.vulcan.model.GoodsPic;
import com.gaosi.api.vulcan.vo.ConfirmOrderVo;
import org.junit.Test;

import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Administrator on 2017/7/11.
 */
public class OtherTest {
    @Test
    public void fun1() throws Exception{
        GoodsOrder temp = new GoodsOrder();
        ExpressCodeUtil.convertExpressCode(temp);
        ExpressCodeUtil.convertExpressCode(temp);
        ExpressCodeUtil.convertExpressCode(temp);
//        Double amount = (245.88d+5.30d) * 10000;
//        Long remain = 2511800l;
//        if (amount.longValue() > remain) {
//            System.out.println("aaaaaaaaaaaaaa");
//        }
//        CustomObjectMapper customObjectMapper=new CustomObjectMapper();
//        GoodsOrderVo gov = new GoodsOrderVo();
//        gov.setConsumeAmount(null);
//
//        //ConfirmOrderVo vo=new ConfirmOrderVo();
//        StringWriter sw = new StringWriter();
//        customObjectMapper.writeValue(sw, gov);
//        ;
////        Double d1 = new Double("12.444");
////        Integer i1 = new Integer("1");
//        System.out.println( sw.getBuffer().toString());

//        DecimalFormat df1 = new DecimalFormat("0.00");
//        GoodsOrderVo goodsOrderVo = new GoodsOrderVo();
//        goodsOrderVo.setFreight(100D);
//        goodsOrderVo.setFreightDis(df1.format(goodsOrderVo.getFreight()));
//        System.out.println(goodsOrderVo.getFreightDis());
    }

    @Test
    public void sortTest(){
        List<GoodsPic> goodsPicList = new ArrayList<>();
        GoodsPic gp1 = new GoodsPic();
        gp1.setPicUrl("aaaa");
        gp1.setMaster(true);
        GoodsPic gp2 = new GoodsPic();
        gp2.setPicUrl("bbbb");
        gp2.setMaster(false);
        GoodsPic gp3= new GoodsPic();
        gp3.setPicUrl("cccc");
        gp3.setMaster(false);
        goodsPicList.add(gp3);
        goodsPicList.add(gp2);
        goodsPicList.add(gp1);
        Collections.sort(goodsPicList);
        for(GoodsPic goodsPic:goodsPicList){
            System.out.println(goodsPic.getPicUrl());
        }
    }
}
