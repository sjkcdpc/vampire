package com.aixuexi.vampire.util;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;
import com.baidu.disconf.client.common.annotations.DisconfUpdateService;
import com.baidu.disconf.client.common.update.IDisconfUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by gaoxinzhong on 2017/5/16.
 */
@Component("expressUtil")
@Scope("singleton")
@DisconfFile(filename = "config.properties")
@DisconfUpdateService(classes = { ExpressUtil.class })
public class ExpressUtil implements IDisconfUpdate {

    private final Logger logger = LoggerFactory.getLogger(ExpressUtil.class);
    /**
     * 拆单提示
     */
    @Value("splitTips")
    private String splitTips;
    /**
     * 时效
     */
    @Value("aging")
    private String aging;
    /**
     * DIY提示
     */
    @Value("diyTips")
    private String diyTips;
    /**
     * 发货时间
     */
    @Value("deliveryTime")
    private String deliveryTime;
    /**
     * 预售发货时间
     */
    @Value("preSaleDeliveryTime")
    private String preSaleDeliveryTime;

    @DisconfFileItem(name = "splitTips", associateField = "splitTips")
    public String getSplitTips() {
        return splitTips;
    }

    @DisconfFileItem(name = "aging", associateField = "aging")
    public String getAging() {
        return aging;
    }

    @DisconfFileItem(name = "diyTips", associateField = "diyTips")
    public String getDiyTips() {
        return diyTips;
    }

    @DisconfFileItem(name = "deliveryTime", associateField = "deliveryTime")
    public String getDeliveryTime() {
        return deliveryTime;
    }

    @DisconfFileItem(name = "preSaleDeliveryTime", associateField = "preSaleDeliveryTime")
    public String getPreSaleDeliveryTime() {
        return preSaleDeliveryTime;
    }

    @Override
    public String toString() {
        return "ExpressUtil{" +
                "splitTips='" + splitTips + '\'' +
                ", aging='" + aging + '\'' +
                ", diyTips='" + diyTips + '\'' +
                ", deliveryTime='" + deliveryTime + '\'' +
                ", preSaleDeliveryTime='" + preSaleDeliveryTime + '\'' +
                '}';
    }

    @Override
    public void reload() throws Exception {
        logger.info(toString());
    }
}
