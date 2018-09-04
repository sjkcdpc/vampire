package com.aixuexi.vampire.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruanyanjie on 2018/8/20.
 */
@RestController
public class MonitorController {

    /**
     * 用于运维监控web服务是否存活
     * @return
     */
    @RequestMapping(value = "/statusCheck",method = RequestMethod.GET)
    public Object get(){
        Map<String,Object> result = new HashMap<>();
        result.put("project", "vampire");
        result.put("statusCode", 200);
        result.put("message",  "None");
        return result;
    }
}
