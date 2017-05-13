package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by gaoxinzhong on 2017/5/13.
 */
@RestController
@RequestMapping(value = "/user")
public class UserController {

    /**
     * 用户信息
     *
     * @return
     */
    @RequestMapping(value = "/loginInfo")
    public ResultData loginInfo() {
        ResultData resultData = new ResultData();
        resultData.setBody("hello world");
        return resultData;
    }
}
