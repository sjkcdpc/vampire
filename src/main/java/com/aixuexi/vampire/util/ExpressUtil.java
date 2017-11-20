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
     * 新运费更新时间
     */
    private String freightUpdateTime;
    /**
     * 拆单提示
     */
    private String splitTips;
    /**
     * 时效
     */
    private String aging;
    /**
     * 快递提示
     */
    private String expressTips;

    public Boolean getSyncToWms() {
        return syncToWms;
    }

    public void setSyncToWms(Boolean syncToWms) {
        this.syncToWms = syncToWms;
    }

    public String getFreightUpdateTime() {
        return freightUpdateTime;
    }

    public void setFreightUpdateTime(String freightUpdateTime) {
        this.freightUpdateTime = freightUpdateTime;
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

    public String getExpressTips() {
        return expressTips;
    }

    public void setExpressTips(String expressTips) {
        this.expressTips = expressTips;
    }
}
