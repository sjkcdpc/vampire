package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.ConsigneeServiceFacade;
import com.gaosi.api.revolver.model.Consignee;
import com.gaosi.api.revolver.vo.ConsigneeVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址管理
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/consignee")
public class ConsigneeController {

    @Autowired
    private ConsigneeServiceFacade consigneeServiceFacade;

    @Autowired
    private AreaApi areaApi;

    /**
     * 保存收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/save")
    public ResultData save(Consignee consignee) {
        ResultData resultData = new ResultData();
        consignee.setInstitutionId(UserHandleUtil.getInsId());
        int id = consigneeServiceFacade.insert(consignee);
        Consignee resConsignee = consigneeServiceFacade.selectById(id);
        String jsonString = JSONObject.toJSONString(resConsignee);
        ConsigneeVo consigneeVo = JSONObject.parseObject(jsonString, ConsigneeVo.class);
        ApiResponse<List<AddressDTO>> apiResponse = areaApi.findAddressByIds(consigneeVo.getAreaId());
        if (CollectionUtils.isNotEmpty(apiResponse.getBody())) {
            AddressDTO addressDTO = apiResponse.getBody().get(0);
            consigneeVo.setProvinceId(addressDTO.getProvinceId());
            consigneeVo.setProvince(addressDTO.getProvince());
            consigneeVo.setCityId(addressDTO.getCityId());
            consigneeVo.setCity(addressDTO.getCity());
            consigneeVo.setArea(addressDTO.getDistrict());
        }
        resultData.setBody(consigneeVo);
        return resultData;
    }

    /**
     * 更新收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/update")
    public ResultData update(Consignee consignee) {
        ResultData resultData = new ResultData();
        consignee.setInstitutionId(UserHandleUtil.getInsId());
        resultData.setBody(consigneeServiceFacade.update(consignee));
        return resultData;
    }

    /**
     * 删除收货地址
     *
     * @param id 收货地址ID
     * @return
     */
    @RequestMapping(value = "/delete")
    public ResultData delete(@RequestParam Integer id) {
        ResultData resultData = new ResultData();
        resultData.setBody(consigneeServiceFacade.delete(id));
        return resultData;
    }


}
