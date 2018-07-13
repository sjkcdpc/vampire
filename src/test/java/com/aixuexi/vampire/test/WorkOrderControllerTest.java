package com.aixuexi.vampire.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Created by ruanyanjie on 2018/7/13.
 */
public class WorkOrderControllerTest  extends BaseTest {
    private static Logger logger = LoggerFactory.getLogger(WorkOrderControllerTest.class);
    @Test
    public void queryRefundList() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/workOrder/refund")
                .param("pageNum","1")
                .param("pageSize","10")
        );
    }

    @Test
    public void afterSales() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/workOrder/afterSales")
                .param("oldOrderId","18051614243942122568")
                .param("mallSkuId","1283")
        );
    }

    @Test
    public void queryRefundDetail() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/workOrder/refund/detail")
                .param("workOrderCode","31804121615091625080")
        );
    }
}
