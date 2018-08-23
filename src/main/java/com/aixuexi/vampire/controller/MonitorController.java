package com.aixuexi.vampire.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruanyanjie on 2018/8/20.
 */
@RestController
@RequestMapping(value = "/monitor")
public class MonitorController {

    /**
     * 用于运维监控web服务是否存活
     * @return
     */
    @RequestMapping(value = "/get/nologin",method = RequestMethod.GET)
    public Object getNoLogin(){
        Map<String,String> result = new HashMap<>();
        result.put("project_name","vampire");
        result.put("project_status","1");
        return result;
    }
}
