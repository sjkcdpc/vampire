package com.aixuexi.vampire.test;

import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.revolver.vo.TalentOrderVo;
import com.gaosi.api.revolver.vo.TalentTemplateVo;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

public class ItemOrderControllerTest extends BaseTest {

    @Test
    public void queryList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                get("/itemOrder/list")
                .param("categoryId", "1")
        );
    }

    @Test
    public void nailSubmit() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                post("/itemOrder/submit")
                .param("itemId", "753")
                .param("itemCount", "2")
        );
    }

    @Test
    public void customServiceSubmit() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.
                post("/itemOrder/customService/submit")
                .param("itemId", "814")
                .param("itemCount", "2")
        );
    }

    @Test
    public void talentCenterSubmit() throws Exception {
        TalentOrderVo talentOrderVo = new TalentOrderVo();
        talentOrderVo.setMallItemId(815);
        talentOrderVo.setMallSkuId(1407);
        talentOrderVo.setNum(2);
        List<TalentTemplateVo> talentTemplateVos = new ArrayList<>();
        TalentTemplateVo talentTemplateVo = new TalentTemplateVo();
        talentTemplateVo.setFieldCode("aaa");
        talentTemplateVo.setFieldLabel("啊啊啊");
        talentTemplateVo.setFieldName("aaabbb");
        talentTemplateVo.setFieldValue("aaabbbccc");
        talentTemplateVos.add(talentTemplateVo);
        talentOrderVo.setTalentTemplateVos(talentTemplateVos);
        String temp = JSONObject.toJSONString(talentOrderVo);
        mockMvc.perform(MockMvcRequestBuilders.
                post("/itemOrder/talentCenter/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(temp)
        );
    }

}
