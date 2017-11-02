package com.aixuexi.vampire.test;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class ItemOrderControllerTest extends BaseTest {

    @Test
    public void queryList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/itemOrder/list")
                .param("categoryId", "1")
        );
    }

}
