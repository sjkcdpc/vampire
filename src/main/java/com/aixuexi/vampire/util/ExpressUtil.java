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
}
