package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.gaosi.api.davincicode.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.User;
import com.gaosi.api.firstblood.constants.Constant;
import com.gaosi.api.firstblood.constants.WhitelistEnum;
import com.gaosi.api.firstblood.model.WhitelistCheck;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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
    @Resource
    private com.gaosi.api.independenceDay.service.UserService userServiceIndependenceDay;

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
                Integer institutionId = user.getInstitutionId();
                result.put("username", user.getName());
                result.put("userId", user.getId());
                result.put("institutionId", institutionId);
                result.put("telephone", user.getTelephone());
                List<String> permissions = UserSessionHandler.getPermissions();
                List<Integer> insIds = Lists.newArrayList();
                insIds.add(institutionId);
                WhitelistCheck whitelistCheck = new WhitelistCheck();
                whitelistCheck.setWhitelistType(WhitelistEnum.institution);
                whitelistCheck.setCheckData(insIds);
                List<WhitelistCheck> whitelistChecks = Lists.newArrayList();
                whitelistChecks.add(whitelistCheck);
                permissions = userServiceIndependenceDay.filterPermissions(permissions, Constant.WHITELISTBUSINESS_HEADMASTER, whitelistChecks);
                Map<String, Integer> menu = new HashMap<>();
                for (String p : permissions) {
                    menu.put(p, 1);
                }
                // 高斯杯权限
                if (userService.checkInstitutionHaveGsb(institutionId)) {
                    menu.put("gaosibei", 1);
                }
                result.put("menu", menu);
                result.put("roles", UserSessionHandler.getRoles());
                resultData.setBody(result);
            } else {
                resultData = ResultData.failed("user not exists, by userId : " + UserSessionHandler.getId());
            }
        } catch (Throwable e) {
            resultData = ResultData.failed(e.getMessage());
        }
        return resultData;
    }
}
