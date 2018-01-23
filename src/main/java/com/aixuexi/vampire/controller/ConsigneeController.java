package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.validate.annotation.common.Valid;
import com.aixuexi.vampire.exception.BusinessException;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.basicdata.DistrictApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.constant.ValidateConstatnt;
import com.gaosi.api.vulcan.facade.ConsigneeServiceFacade;
import com.gaosi.api.vulcan.model.Consignee;
import com.gaosi.api.vulcan.vo.ConsigneeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 收货地址管理
 * Created by gaoxinzhong on 2017/5/15.
 */
@RestController
@RequestMapping(value = "/consignee")
public class ConsigneeController {

    @Autowired
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
        int id = consigneeServiceFacade.insert(UserHandleUtil.getInsId(), consignee);
        resultData.setBody(getConsigneeVoById(id));
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
        ApiResponse<Integer> apiResponse = consigneeServiceFacade.update(UserHandleUtil.getInsId(), consignee);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        ConsigneeVo cv = getConsigneeVoById(consignee.getId());
        if (cv == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "该收货人不存在");
        }
        resultData.setBody(cv);
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
        ApiResponse<Integer> apiResponse = consigneeServiceFacade.delete(UserHandleUtil.getInsId(), id);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        Integer effectRows = apiResponse.getBody();
        if (effectRows <= 0) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "该收货人不存在");
        } else {
            resultData.setBody(effectRows);
        }
        return resultData;
    }

    /**
     * 返回新增或更新后的收货人信息
     *
     * @param id 收货人ID
     * @return
     */
    private ConsigneeVo getConsigneeVoById(int id) {
        Consignee resConsignee = consigneeServiceFacade.selectById(id);
        if (resConsignee == null) {
            return null;
        }
        ConsigneeVo consigneeVo = baseMapper.map(resConsignee, ConsigneeVo.class);
        Integer areaId = consigneeVo.getAreaId();
        ApiResponse<AddressDTO> apiResponse = districtApi.getAncestryById(areaId);
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
        ResultData resultData = new ResultData();
        ApiResponse<Integer> apiResponse = consigneeServiceFacade.setDefault(UserHandleUtil.getInsId(), id);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, apiResponse.getMessage());
        }
        Integer effectRows = apiResponse.getBody();
        if (effectRows <= 0) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "该收货人不存在");
        } else {
            resultData.setBody(effectRows);
        }
        return resultData;
    }
}
