package com.aixuexi.vampire.test;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Created by ruanyanjie on 2018/8/20.
 */
public class MonitorControllerTest extends BaseTest {

    @Test
    public void getNoLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/statusCheck")
        );
    }

}
