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
                .param("schemeId","0")
                .param("subjectId","2")
                .param("subjectProductId","0")
                .param("periodId","0")
                .param("categoryId","0")
        );
    }

    @Test
    public void querylist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/goods/list")
                .param("pageNum","1")
                .param("pageSize","10")
                //.param("schemeId","0")
                .param("subjectId","2")
                .param("subjectProductId","7")
                .param("periodId","3")
                .param("categoryId","1")
        );
    }

    @Test
    public void queryGoodsList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/goods/goodsList")
                .param("sid","0")
                .param("pid","0")
                .param("vtId","0")
                .param("veId","0")
                .param("pageNum","1")
                .param("pageSize","10")
        );
    }

    @Test
    public void getByGoodsName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/goods/getByGoodsName")
                .param("goodName","baopan")
                .param("pageNum","1")
                .param("pageSize","10")
        );
    }
}
