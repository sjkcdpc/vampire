package com.aixuexi.vampire.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaosi.api.revolver.model.LogisticsData;
import com.gaosi.api.revolver.util.JsonUtil;
import org.junit.Test;

import java.util.List;

/**
 * Created by Administrator on 2017/7/11.
 */
public class OtherTest {
    @Test
    public void fun1(){
       String str="{\"data\":[{\"context\":\"泰安 的 翟汗君7175741 正在派件\",\"time\":\"2016-11-16 07:11:09\"},{\"context\":\"泰安的派件已签收，感谢您使用中通快递！\",\"time\":\"2016-11-16 06:11:35\"},{\"context\":\"快件离开 济南中转部 已发往 泰安\",\"time\":\"2016-11-15 10:11:53\"},{\"context\":\"快件已到达 济南中转部\",\"time\":\"2016-11-15 09:11:31\"},{\"context\":\"快件已到达 泰安\",\"time\":\"2016-11-15 04:11:10\"},{\"context\":\"快件离开 北京 已发往 济南中转部\",\"time\":\"2016-11-14 11:11:46\"},{\"context\":\"快件已到达 北京\",\"time\":\"2016-11-14 11:11:36\"},{\"context\":\"学清路 的 赵中铁 已收件\",\"time\":\"2016-11-14 07:11:15\"},{\"context\":\"快件离开 学清路 已发往 北京\",\"time\":\"2016-11-14 06:11:28\"}]}";
        List<LogisticsData> logisticsData = JsonUtil.jsonToList(str, LogisticsData.class);
        System.out.println(logisticsData);
    }
}
