package com.aixuexi.vampire.util;

import com.gaosi.api.revolver.vo.ExpressVo;
import com.gaosi.api.vulcan.vo.ConfirmExpressVo;

import java.util.List;

/**
 * Created by gaoxinzhong on 2017/5/16.
 */
public class ExpressUtil {
    /**
     * 配送方式
     */
    private List<ConfirmExpressVo> express;

    /**
     * 是否走发网
     */
    private Boolean syncToWms;

    /**
     * 是否库存销售
     */
    private Boolean isInventory;

    /**
     * 新运费更新时间
     */
    private String freightUpdateTime;
    /**
     * 快递信息
     */
    private List<ExpressVo> expressVos;
    /**
     * 拆单提示
     */
    private String splitTips;

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

    public List<ExpressVo> getExpressVos() {
        return expressVos;
    }

    public void setExpressVos(List<ExpressVo> expressVos) {
        this.expressVos = expressVos;
    }

    public String getSplitTips() {
        return splitTips;
    }

    public void setSplitTips(String splitTips) {
        this.splitTips = splitTips;
    }
}
