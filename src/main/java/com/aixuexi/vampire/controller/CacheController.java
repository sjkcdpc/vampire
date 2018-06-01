package com.aixuexi.vampire.controller;

import com.aixuexi.vampire.manager.CacheManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Created by ruanyanjie on 2018/6/1.
 */
@RestController
@RequestMapping(value = "/cache")
public class CacheController {

    @Resource
    private CacheManager cacheManager;

    /**
     * 清空缓存
     */
    @RequestMapping(value = "/refresh",method = RequestMethod.GET)
    public void invalidateAllCache() {
        cacheManager.invalidateAll();
    }
}
