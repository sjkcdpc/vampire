package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.response.StorageResponse;
import com.aixuexi.thor.storage.StorageAuthorizationUtil;
import com.gaosi.api.vulcan.util.PicUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author baopan
 * @createTime 20181010
 */
@RestController
@RequestMapping("/storage")
public class StorageController {

    @Value("${storage.ak}")
    private String accessKey;
    @Value("${storage.sk}")
    private String secretKey;

    /**
     * 15分钟
     */
    private static long expiredTime = 15 * 60 * 1000;

    /**
     * 存储：获取授权信息
     *
     * @return
     */
    @RequestMapping(value = "/signature", method = RequestMethod.GET)
    public ResultData getSignature() {
        Date expired = new Date(System.currentTimeMillis() + expiredTime);
        StorageResponse<String> signatureResponse = StorageAuthorizationUtil.getSignature(accessKey, secretKey, expired);
        String signature = signatureResponse.getBody();

        Map<String, String> result = new HashMap<>();
        result.put("token", signature);
        result.put("businessKey", PicUtils.MALL_STORAGE);

        return ResultData.successed(result);
    }
}
