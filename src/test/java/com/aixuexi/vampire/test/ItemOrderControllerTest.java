package com.aixuexi.vampire.test;

import com.aixuexi.vampire.manager.ItemOrderManager;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.annotation.Resource;

/**
 * @Description:
 * @Author: liuxinyun
 * @Date: 2017/9/11 10:41
 */
public class ItemOrderControllerTest extends BaseTest {

    @Resource
    private FinancialAccountService finAccService;
    @Resource(name = "itemOrderManager")
    private ItemOrderManager itemOrderManager;

    @Test
    public void costTest(){
        String token = finAccService.getTokenForFinancial();
        String orderId = "1709141414113758";
        itemOrderManager.pay(orderId, token);
    }

    @Test
    public void queryList() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.
                get("/itemOrder/list")
                .param("categoryId","1")
        );
    }

}
