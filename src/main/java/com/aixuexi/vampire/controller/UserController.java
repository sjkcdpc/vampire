package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.User;
import com.gaosi.api.firstblood.constants.Constant;
import com.gaosi.api.firstblood.constants.WhitelistEnum;
import com.gaosi.api.firstblood.model.WhitelistCheck;
import com.gaosi.api.independenceDay.service.GroupInstitutionService;
import com.gaosi.api.vulcan.bean.common.QueryCriteria;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.vo.MallItemCustomServiceVo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Resource
    private UserService userService;
    @Resource
    private com.gaosi.api.independenceDay.service.UserService userServiceIndependenceDay;
    @Resource
    private MallItemExtServiceFacade mallItemExtServiceFacade;
    @Resource
    private GroupInstitutionService groupInstitutionService;


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
                // 判断定制服务的目录是否展示
                QueryCriteria queryCriteria = new QueryCriteria();
                queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
                queryCriteria.setCategoryId(MallItemConstant.Category.DZFW.getId());
                ApiResponse<Page<MallItemCustomServiceVo>> apiResponse = mallItemExtServiceFacade.queryMallItemList4DZFW(queryCriteria, institutionId);
                Boolean customService = false;
                if (apiResponse.getRetCode() == ApiRetCode.SUCCESS_CODE&& CollectionUtils.isNotEmpty(apiResponse.getBody().getList())) {
                    customService = true;
                }
                // 机构组织类型（0: 普通机构, 1:集团, 2: 分校）
                int groupInsOrgType = groupInstitutionService.getGroupInsOrgType(institutionId);
                result.put("groupInsOrgType",groupInsOrgType);
                result.put("username", user.getName());
                result.put("userId", user.getId());
                result.put("institutionId", institutionId);
                result.put("telephone", user.getTelephone());
                result.put("customService", customService);
                List<String> permissions = UserSessionHandler.getPermissions();
                List<Integer> insIds = Lists.newArrayList();
                insIds.add(institutionId);
                WhitelistCheck whitelistCheck = new WhitelistCheck();
                whitelistCheck.setWhitelistType(WhitelistEnum.institution);
                whitelistCheck.setCheckData(insIds);
                List<WhitelistCheck> whitelistChecks = Lists.newArrayList();
                whitelistChecks.add(whitelistCheck);
                /** 获取杯赛权限 */
                List<String> cups = userService.checkInstitutionHaveCup(institutionId);
                if(cups != null) {
                    permissions.addAll(cups);
                }
                permissions = userServiceIndependenceDay.filterPermissions(permissions, Constant.WHITELISTBUSINESS_HEADMASTER, whitelistChecks);
                Map<String, Integer> menu = new HashMap<>();
                for (String p : permissions) {
                    menu.put(p, 1);
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
