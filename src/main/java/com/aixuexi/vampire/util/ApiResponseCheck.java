package com.aixuexi.vampire.util;

import com.gaosi.api.common.to.ApiResponse;
import org.springframework.util.Assert;

/**
 * @author zhouxiong
 * on 2017/8/22 15:37.
 */
public class ApiResponseCheck {
    /**
     * 通用apiResponse校验(非空，以及ApiRetCode为SUCCESS)
     *
     * @param apiResponse
     * @Author zhouxiong
     * @Date 2017/6/8 13:25
     */
    public static void check(ApiResponse apiResponse) {
        Assert.notNull(apiResponse, "apiResponse is null.");
        Assert.isTrue(apiResponse.isSuccess(), apiResponse.getMessage());
    }

    /**
     * 新apiResponse校验(非空，以及ApiRetCode为SUCCESS)
     * @param apiResponse
     */
    public static void checkNew(com.aixuexi.thor.response.ApiResponse apiResponse) {
        Assert.notNull(apiResponse, "apiResponse is null.");
        Assert.isTrue(apiResponse.isSuccess(), apiResponse.getMessage());
    }
}
