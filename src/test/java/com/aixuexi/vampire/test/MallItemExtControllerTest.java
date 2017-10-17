package com.aixuexi.vampire.test;

import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Created by ruanyanjie on 2017/10/13.
 */
public class MallItemExtControllerTest extends BaseTest {

    @Test
    public void queryMallItemNailList() throws Exception{
        long start = System.currentTimeMillis();
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/nail")
                .param("pageNum","1")
                .param("pageSize","10")
        );
        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }
}
