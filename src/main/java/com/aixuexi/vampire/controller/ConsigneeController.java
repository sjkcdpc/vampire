package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.gaosi.api.revolver.model.Consignee;
import com.gaosi.api.revolver.facade.ConsigneeServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收货地址管理
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/consignee")
public class ConsigneeController {

    @Autowired
    private ConsigneeServiceFacade consigneeServiceFacade;

    /**
     * 保存收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/save")
    public ResultData save(Consignee consignee) {
        ResultData resultData = new ResultData();
        resultData.setBody(consigneeServiceFacade.insert(consignee));
        return resultData;
    }

    /**
     * 更新收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/update")
    public ResultData update(Consignee consignee) {
        ResultData resultData = new ResultData();
        resultData.setBody(consigneeServiceFacade.update(consignee));
        return resultData;
    }
}
