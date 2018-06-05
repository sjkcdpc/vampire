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
        List<Integer> integers = Lists.newArrayList(1, 2, 3, 0);
        if(integers.contains(0)){
            integers.remove(Integer.valueOf(0));
        }
        System.out.println(integers);

//        Map<Integer, Integer> m1 = new HashMap(1);
//        m1.put(1, 1);
//        m1.put(2, 2);
//        m1.put(3, 3);
//        System.out.println(m1.size());
//        List<String> list = new ArrayList<String>();
//        list.add("1");
//        list.add("2");
//        for (String item : list) {
//            if ("2".equals(item)) {
//                list.remove(item);
//            }
//        }
//        System.out.println(list.size());
//        System.out.println(list.get(0));

//        String str = "a,b,c,,";
//        String[] ary = str.split(",");
//        // 预期大于 3，结果是 3
//        System.out.println(ary.length);
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
