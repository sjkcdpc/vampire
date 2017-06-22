package com.aixuexi.vampire.util;

import com.gaosi.api.revolver.vo.ConfirmExpressVo;

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
}
