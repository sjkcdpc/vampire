package com.aixuexi.vampire.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Created by ruanyanjie on 2017/10/13.
 */
public class MallItemExtControllerTest extends BaseTest {
    private static Logger logger = LoggerFactory.getLogger(MallItemExtControllerTest.class);
    
    @Test
    public void queryMallItemNailList() throws Exception{
        long start = System.currentTimeMillis();
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/nail")
                .param("pageNum","1")
                .param("pageSize","10")
        );
        long end = System.currentTimeMillis();
        logger.info(""+(end-start));
    }

    @Test
    public void queryMallItemNail() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/nail/detail")
                .param("mallItemId","379")
        );
    }

    @Test
    public void queryCondition4TalentCenter() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/talentCenter/queryCondition")
        );
    }

    @Test
    public void queryTalentCenterList() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/talentCenter/list")
        );
    }

    @Test
    public void queryTalentCenterDetail() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/talentCenter/detail").
                param("mallItemId","815")
        );
    }

    @Test
    public void confirmTalentCenter() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/item/talentCenter/confirm").
                param("mallItemId","815").
                param("mallSkuId","1407").
                param("num","2")
        );
    }
}
