package com.aixuexi.vampire.test;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author zhouxiong
 * on 2017/8/23 13:35.
 */
public class GoodsControllerTest extends BaseTest {
    @Test
    public void partQueryCondition() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/goods/partQueryCondition")
        );
    }

    @Test
    public void queryCondition() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/goods/queryCondition")
                .param("subjectId","2")
        );
    }
}
