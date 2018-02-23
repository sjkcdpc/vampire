package com.aixuexi.vampire.test;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Created by ruanyanjie on 2018/2/1.
 */
public class OrderControllerTest extends BaseTest {
    @Test
    public void confirm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/order/confirm")
        );
    }
}
