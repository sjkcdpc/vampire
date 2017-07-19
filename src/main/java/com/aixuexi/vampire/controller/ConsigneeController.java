package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.basicdata.AreaApi;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.facade.ConsigneeServiceFacade;
import com.gaosi.api.vulcan.model.Consignee;
import com.gaosi.api.vulcan.vo.ConsigneeVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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

    @Resource
    private BaseMapper baseMapper;

    /**
     * 保存收货地址
     *
     * @param consignee 收货地址
     * @return
     */
    @RequestMapping(value = "/save",method = RequestMethod.POST)
    public ResultData save(@RequestBody Consignee consignee) {
        ResultData resultData = new ResultData();
        consignee.setInstitutionId(UserHandleUtil.getInsId());
        int id = consigneeServiceFacade.insert(consignee);
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
    public ResultData update(Consignee consignee) {
        ResultData resultData = new ResultData();
        consignee.setInstitutionId(UserHandleUtil.getInsId());
        consigneeServiceFacade.update(consignee);
        ConsigneeVo cv = getConsigneeVoById(consignee.getId());
        if(cv==null) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "该收货人不存在");
        }
        else {
            resultData.setBody(cv);
        }
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
        int res = consigneeServiceFacade.delete(id);
        if(res<=0) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "该收货人不存在");
        }
        else {
            resultData.setBody(res);
        }
        return resultData;
    }
    /**
     * 返回新增或更新后的收货人信息
     *
     * @param id 收货人ID
     * @return
     */
    private ConsigneeVo getConsigneeVoById(int id)
    {
        Consignee resConsignee = consigneeServiceFacade.selectById(id);
        if(resConsignee!=null) {

            ConsigneeVo consigneeVo = baseMapper.map(resConsignee, ConsigneeVo.class);
            ApiResponse<List<AddressDTO>> apiResponse = areaApi.findAddressByIds(consigneeVo.getAreaId());
            if (CollectionUtils.isNotEmpty(apiResponse.getBody())) {
                AddressDTO addressDTO = apiResponse.getBody().get(0);
                consigneeVo.setProvinceId(addressDTO.getProvinceId());
                consigneeVo.setProvince(addressDTO.getProvince());
                consigneeVo.setCityId(addressDTO.getCityId());
                consigneeVo.setCity(addressDTO.getCity());
                consigneeVo.setArea(addressDTO.getDistrict());
            }
            return consigneeVo;
        }
        return null;
    }


}
