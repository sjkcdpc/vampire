package com.aixuexi.vampire.util;

import com.gaosi.api.common.constants.AccessConstant;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;

/**
 * Created by gaoxinzhong on 2017/6/22.
 */
public final class UserHandleUtil {

    private UserHandleUtil() {
    }

    /**
     * 机构ID
     *
     * @return
     */
    public static Integer getInsId() {
        return Integer.parseInt(UserSessionHandler.get(AccessConstant.USER_INSTITUTION_ID_KEY));
    }

    /**
     * 用户ID
     *
     * @return
     */
    public static Integer getUserId() {
        return Integer.parseInt(UserSessionHandler.get(AccessConstant.USER_ID_KEY));
    }
}
