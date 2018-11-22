package com.aixuexi.vampire.util;

/**
 * Created by gaoxinzhong on 2017/5/16.
 */
public class ExpressUtil {

    /**
     * 是否走发网
     */
    private Boolean syncToWms;
    /**
     * 拆单提示
     */
    private String splitTips;
    /**
     * 时效
     */
    private String aging;
    /**
     * DIY提示
     */
    private String diyTips;
    /**
     * 发货时间
     */
    private String deliveryTime;
    /**
     * 预售发货时间
     */
    private String preSaleDeliveryTime;

    public Boolean getSyncToWms() {
        return syncToWms;
    }

    public void setSyncToWms(Boolean syncToWms) {
        this.syncToWms = syncToWms;
    }

    public String getSplitTips() {
        return splitTips;
    }

    public void setSplitTips(String splitTips) {
        this.splitTips = splitTips;
    }

    public String getAging() {
        return aging;
    }

    public void setAging(String aging) {
        this.aging = aging;
    }

    public String getDiyTips() {
        return diyTips;
    }

    public void setDiyTips(String diyTips) {
        this.diyTips = diyTips;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getPreSaleDeliveryTime() {
        return preSaleDeliveryTime;
    }

    public void setPreSaleDeliveryTime(String preSaleDeliveryTime) {
        this.preSaleDeliveryTime = preSaleDeliveryTime;
    }
}
