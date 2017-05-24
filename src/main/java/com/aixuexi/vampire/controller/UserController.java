package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.gaosi.api.davincicode.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.User;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoxinzhong on 2017/5/13.
 */
@RestController
@RequestMapping(value = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户信息
     *
     * @return
     */
    @RequestMapping(value = "/loginInfo")
    public ResultData loginInfo() {
        ResultData resultData = new ResultData();
        try {
            User user = userService.getUserById(UserSessionHandler.getId());
            if (null != user) {
                Map<String, Object> result = Maps.newHashMap();
                result.put("username", user.getName());
                result.put("userId", user.getId());
                result.put("institutionId", user.getInstitutionId());
                result.put("telephone", user.getTelephone());
                List<String> permissions = UserSessionHandler.getPermissions();
                Map<String, Integer> menu = new HashMap<>();
                for (String p : permissions) {
                    menu.put(p, 1);
                }
                // 高斯杯权限
                if (userService.checkInstitutionHaveGsb(user.getInstitutionId())) {
                    menu.put("gaosibei", 1);
                }
                result.put("menu", menu);
                result.put("roles", UserSessionHandler.getRoles());
                resultData.setBody(result);
            } else {
                resultData.setBody(null);
                resultData.setStatus(0);
                resultData.setErrorMessage("user not exists, by userId : " + UserSessionHandler.getId());
            }
        } catch (Throwable e) {
            resultData.setBody(null);
            resultData.setStatus(0);
            resultData.setErrorMessage(e.getMessage());
        }
        return resultData;
    }
}
