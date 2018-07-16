package com.aixuexi.vampire.test;

import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.revolver.model.WorkOrderPic;
import com.gaosi.api.revolver.vo.WorkOrderRefundDetailVo;
import com.gaosi.api.revolver.vo.WorkOrderRefundVo;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void addRefund() throws Exception{
        WorkOrderRefundVo workOrderRefundVo = new WorkOrderRefundVo();
        workOrderRefundVo.setOldOrderId("1602181215592142");
        workOrderRefundVo.setReasonId(1);
        workOrderRefundVo.setType(2);
        workOrderRefundVo.setDescription("阿道夫暗室逢灯撒反对sad分");
        List<WorkOrderPic> workOrderPics = new ArrayList<>();
        WorkOrderPic workOrderPic = new WorkOrderPic();
        workOrderPic.setPicUrl("http://tupain.jpg");
        workOrderPics.add(workOrderPic);
        workOrderRefundVo.setWorkOrderPics(workOrderPics);
        List<WorkOrderRefundDetailVo> workOrderRefundDetailVos = new ArrayList<>();
        WorkOrderRefundDetailVo workOrderRefundDetailVo = new WorkOrderRefundDetailVo();
        workOrderRefundDetailVo.setMallItemId(9);
        workOrderRefundDetailVo.setMallSkuId(524);
        workOrderRefundDetailVo.setTotalNum(2);
        workOrderRefundDetailVos.add(workOrderRefundDetailVo);
        workOrderRefundVo.setWorkOrderRefundDetailVos(workOrderRefundDetailVos);
        String temp = JSONObject.toJSONString(workOrderRefundVo);
        mockMvc.perform(MockMvcRequestBuilders.
                post("/workOrder/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(temp)
        );
    }

    @Test
    public void updateRefund() throws Exception{
        WorkOrderRefundVo workOrderRefundVo = new WorkOrderRefundVo();
        workOrderRefundVo.setWorkOrderCode("318071619095954532091");
        workOrderRefundVo.setOldOrderId("1602181215592142");
        workOrderRefundVo.setReasonId(8);
        workOrderRefundVo.setType(2);
        workOrderRefundVo.setDescription("阿道夫暗室逢灯撒反对sad分111111");
        List<WorkOrderPic> workOrderPics = new ArrayList<>();
        WorkOrderPic workOrderPic = new WorkOrderPic();
        workOrderPic.setPicUrl("http://tupain1111.jpg");
        workOrderPics.add(workOrderPic);
        workOrderRefundVo.setWorkOrderPics(workOrderPics);
        List<WorkOrderRefundDetailVo> workOrderRefundDetailVos = new ArrayList<>();
        WorkOrderRefundDetailVo workOrderRefundDetailVo = new WorkOrderRefundDetailVo();
        workOrderRefundDetailVo.setMallItemId(9);
        workOrderRefundDetailVo.setMallSkuId(524);
        workOrderRefundDetailVo.setTotalNum(1);
        workOrderRefundDetailVos.add(workOrderRefundDetailVo);
        workOrderRefundVo.setWorkOrderRefundDetailVos(workOrderRefundDetailVos);
        String temp = JSONObject.toJSONString(workOrderRefundVo);
        mockMvc.perform(MockMvcRequestBuilders.
                post("/workOrder/refund/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(temp)
        );
    }
}
