package com.aixuexi.vampire.test;

import com.alibaba.fastjson.JSON;
import com.gaosi.api.vulcan.model.Consignee;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Created by ruanyanjie on 2018/1/22.
 */
public class ConsigneeControllerTest extends BaseTest  {
    @Test
    public void add() throws Exception {
        Consignee consignee = new Consignee();
        String temp = JSON.toJSONString(consignee);
        mockMvc.perform(MockMvcRequestBuilders.
                post("/consignee/save")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(temp)
        );
    }

    @Test
    public void update() throws Exception {
        String temp = "{\"address\":[\"白石镇丽景花园1\"]}";
        mockMvc.perform(MockMvcRequestBuilders.
                post("/consignee/update")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(temp)
        );
    }
}
