package com.aixuexi.vampire.util;

import com.gaosi.api.vulcan.vo.ConfirmExpressVo;

import java.util.List;

/**
 * Created by gaoxinzhong on 2017/5/16.
 */
public class ExpressUtil {

    private List<ConfirmExpressVo> express;

    /**
     * 是否走发网
     */
    private Boolean syncToWms;

    /**
     * 是否库存销售
     */
    private Boolean isInventory;

    private String freightUpdateTime;

    public List<ConfirmExpressVo> getExpress() {
        return express;
    }

    public void setExpress(List<ConfirmExpressVo> express) {
        this.express = express;
    }

    public Boolean getSyncToWms() {
        return syncToWms;
    }

    public void setSyncToWms(Boolean syncToWms) {
        this.syncToWms = syncToWms;
    }

    public Boolean getIsInventory() {
        return isInventory;
    }

    public void setIsInventory(Boolean isInventory) {
        this.isInventory = isInventory;
    }

    public String getFreightUpdateTime() {
        return freightUpdateTime;
    }

    public void setFreightUpdateTime(String freightUpdateTime) {
        this.freightUpdateTime = freightUpdateTime;
    }
}
