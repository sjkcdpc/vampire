package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.manager.CacheManager;
import com.gaosi.api.vulcan.vo.AreaVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 地区
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/area")
public class AreaController {

    private final Logger logger = LoggerFactory.getLogger(AreaController.class);

    @Resource
    private CacheManager cacheManager;


    /**
     * 省
     *
     * @return
     */
    @RequestMapping(value = "/province",method = RequestMethod.GET)
    public ResultData province() {
        ResultData resultData = new ResultData();
        try {
            List<AreaVo> areaVos = cacheManager.getCacheBuilderProvince().get(1);
            resultData.setBody(areaVos);
        } catch (ExecutionException e) {
            logger.error("获取省份信息失败", e);
            resultData = ResultData.failed("获取省份信息失败");
        }
        return resultData;
    }

    /**
     * 市区
     *
     * @param parentId 父级ID
     * @return
     */
    @RequestMapping(value = "/cityArea",method = RequestMethod.GET)
    public ResultData city(@RequestParam Integer parentId) {
        ResultData resultData = new ResultData();
        try {
            List<AreaVo> areaVos = cacheManager.getCacheBuilderCity().get(parentId);
            resultData.setBody(areaVos);
        } catch (ExecutionException e) {
            logger.error("获取市区信息失败", e);
            resultData = ResultData.failed("获取市区信息失败");
        }
        return resultData;
    }


}

