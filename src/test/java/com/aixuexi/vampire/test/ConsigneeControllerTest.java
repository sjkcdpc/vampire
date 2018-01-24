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
        consignee.setName("张三");
        consignee.setAreaId(10007607);
        consignee.setAddress("一个好地方");
        consignee.setPhone("11111111111");
        String temp = JSON.toJSONString(consignee);
        mockMvc.perform(MockMvcRequestBuilders.
                post("/consignee/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(temp)
        );
    }

    @Test
    public void update() throws Exception {
        Consignee consignee = new Consignee();
        consignee.setId(1316);
        consignee.setName("张三3");
        String temp = JSON.toJSONString(consignee);
        mockMvc.perform(MockMvcRequestBuilders.
                get("/consignee/update").param("id","1316").param("name","张三3")
        );
    }
}
