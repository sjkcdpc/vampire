package com.aixuexi.vampire.test;


import com.aixuexi.thor.response.ResultData;
import com.gaosi.api.vulcan.model.GoodsPic;
import com.gaosi.api.vulcan.vo.CommonConditionVo;
import com.gaosi.api.vulcan.vo.GoodsConditionVo;
import com.google.common.collect.Lists;
import net.sf.json.JSONObject;
import org.junit.Test;

import java.util.*;


/**
 * Created by Administrator on 2017/7/11.
 */
public class OtherTest {
    @Test
    public void fun1() throws Exception {
    }

    @Test
    public void sortTest() {
        List<GoodsPic> goodsPicList = new ArrayList<>();
        GoodsPic gp1 = new GoodsPic();
        gp1.setPicUrl("aaaa");
        gp1.setMaster(true);
        GoodsPic gp2 = new GoodsPic();
        gp2.setPicUrl("bbbb");
        gp2.setMaster(false);
        GoodsPic gp3 = new GoodsPic();
        gp3.setPicUrl("cccc");
        gp3.setMaster(false);
        goodsPicList.add(gp3);
        goodsPicList.add(gp2);
        goodsPicList.add(gp1);
        Collections.sort(goodsPicList);
        for (GoodsPic goodsPic : goodsPicList) {
            System.out.println(goodsPic.getPicUrl());
        }
    }
}
