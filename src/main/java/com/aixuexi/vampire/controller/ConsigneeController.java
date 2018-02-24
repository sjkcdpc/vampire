package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.validate.annotation.common.Valid;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.DistrictApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.constant.ValidateConstatnt;
import com.gaosi.api.vulcan.facade.ConsigneeServiceFacade;
import com.gaosi.api.vulcan.model.Consignee;
import com.gaosi.api.vulcan.vo.ConsigneeVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 收货地址管理
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/consignee")
public class ConsigneeController {

    @Resource
    private ConsigneeServiceFacade consigneeServiceFacade;

    @Resource
    private DistrictApi districtApi;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 保存收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public ResultData save(@RequestBody @Valid(groups = ValidateConstatnt.INSERT_GROUP) Consignee consignee) {
        ResultData resultData = new ResultData();
        consignee.setInstitutionId(UserHandleUtil.getInsId());
        ApiResponse<Consignee> consigneeResponse = consigneeServiceFacade.insert(consignee);
        ApiResponseCheck.check(consigneeResponse);
        Consignee newConsignee = consigneeResponse.getBody();
        resultData.setBody(dealConsignee2Vo(newConsignee));
        return resultData;
    }

    /**
     * 更新收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/update")
    public ResultData update(@Valid(groups = ValidateConstatnt.UPDATE_GROUP) Consignee consignee) {
        ResultData resultData = new ResultData();
        ApiResponse<Consignee> consigneeResponse = consigneeServiceFacade.update(consignee);
        ApiResponseCheck.check(consigneeResponse);
        Consignee newConsignee = consigneeResponse.getBody();
        resultData.setBody(dealConsignee2Vo(newConsignee));
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
        ApiResponse<Integer> apiResponse = consigneeServiceFacade.delete(UserHandleUtil.getInsId(), id);
        ApiResponseCheck.check(apiResponse);
        Integer effectRows = apiResponse.getBody();
        return ResultData.successed(effectRows);
    }

    /**
     * Consignee转ConsigneeVo
     * @param consignee
     * @return
     */
    private ConsigneeVo dealConsignee2Vo(Consignee consignee) {
        ConsigneeVo consigneeVo = baseMapper.map(consignee, ConsigneeVo.class);
        Integer areaId = consigneeVo.getAreaId();
        ApiResponse<AddressDTO> apiResponse = districtApi.getAncestryById(areaId);
        ApiResponseCheck.check(apiResponse);
        AddressDTO addressDTO = apiResponse.getBody();
        if (addressDTO != null) {
            consigneeVo.setProvinceId(addressDTO.getProvinceId());
            consigneeVo.setProvince(addressDTO.getProvince());
            consigneeVo.setCityId(addressDTO.getCityId());
            consigneeVo.setCity(addressDTO.getCity());
            consigneeVo.setArea(addressDTO.getDistrict());
        }
        return consigneeVo;
    }

    /**
     * 设为默认地址
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/setDefault")
    public ResultData setDefault(@RequestParam Integer id) {
        ApiResponse<Integer> apiResponse = consigneeServiceFacade.setDefault(UserHandleUtil.getInsId(), id);
        ApiResponseCheck.check(apiResponse);
        Integer effectRows = apiResponse.getBody();
        return ResultData.successed(effectRows);
    }
}
