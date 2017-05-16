package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.manager.OrderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 订单
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/order")
public class OrderController {

    @Resource(name = "orderManager")
    private OrderManager orderManager;

    /**
     * 订单列表
     *
     * @return
     */
    @RequestMapping(value = "/list")
    public ResultData list() {
        // TODO
        return null;
    }

    /**
     * 订单详情
     *
     * @return
     */
    @RequestMapping(value = "/detail")
    public ResultData detail() {
        // TODO
        return null;
    }

    /**
     * 确认订单
     *
     * @return
     */
    @RequestMapping(value = "/confirm")
    public ResultData confirm(@RequestParam Integer userId, @RequestParam Integer insId) {
        ResultData resultData = new ResultData();
        resultData.setBody(orderManager.confirmOrder(userId, insId));
        return resultData;
    }

    /**
     * 计算运费
     *
     * @return
     */
    @RequestMapping(value = "/freight")
    private ResultData freight() {
        // TODO
        return null;
    }

    /**
     * 提交订单
     *
     * @return
     */
    @RequestMapping(value = "/submit")
    public ResultData submit() {
        // TODO
        return null;
    }

    /**
     * 取消订单
     *
     * @return
     */
    @RequestMapping(value = "/cancel")
    public ResultData cancel() {
        // TODO
        return null;
    }
}

