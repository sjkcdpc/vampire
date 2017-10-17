package com.aixuexi.vampire.test;

import com.aixuexi.thor.sms_mail.SMSConstant;
import com.aixuexi.transformers.mq.ONSMQProducer;
import com.aixuexi.transformers.msg.SmsSend;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

/**
 * @Description:消息队列测试
 * @Author: liuxinyun
 * @Date: 2017/8/29 14:50
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-bean-web.xml",
        "classpath:spring-bean-mq.xml"
})
public class MqTest {

    @Autowired(required = false)
    private ONSMQProducer mqSmsProducer;

    @Value("${order_update_fail_receive_phone}")
    private String phoneStr;

    @Test
    public void smsTest(){
        String[] phones = phoneStr.split(",");
        SmsSend.SmsSendObject.Builder builder = SmsSend.SmsSendObject.newBuilder();
        builder.setSignName(SMSConstant.SIGN_AIXUEXI)
                .putParam("orderId", "110119120")
                .setTemplateCode(SMSConstant.TEMPLATE_CODE_ORDER_UPDATE_FAIL_NOTIFY)
                .addAllPhones(Arrays.asList(phones))
                .setBusinessType(SMSConstant.BUSINESS_TYPE_ORDER_UPDATE_FAIL_NOTIFY);
        mqSmsProducer.send(builder);
    }
}
