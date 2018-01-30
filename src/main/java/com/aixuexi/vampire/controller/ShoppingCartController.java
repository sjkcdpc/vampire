package com.aixuexi.vampire.controller;

import com.aixuexi.thor.redis.MyJedisService;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.util.*;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.constant.GoodsConstant;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.facade.GoodsTypeServiceFacade;
import com.gaosi.api.vulcan.facade.ShoppingCartServiceFacade;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.model.ShoppingCartList;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.ShoppingCartListVo;
import com.gaosi.api.vulcan.vo.ShoppingCartVo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoxinzhong on 2017/5/24.
 */
@RestController
@RequestMapping(value = "/shoppingCart")
public class ShoppingCartController {

    @Resource
    private ShoppingCartServiceFacade shoppingCartServiceFacade;

    @Resource
    private GoodsServiceFacade goodsServiceFacade;

    @Resource
    private MyJedisService myJedisService;

    @Resource
    private GoodsTypeServiceFacade goodsTypeServiceFacade;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 购物车
     *
     * @return
     */
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    public ResultData list() {
        ResultData resultData = new ResultData();

        Integer userId = UserHandleUtil.getUserId();
        List<ShoppingCartList> shoppingCartListList = shoppingCartServiceFacade.queryShoppingCartDetail(userId);
        if (CollectionUtils.isEmpty(shoppingCartListList)) {
            return resultData;
        }
        List<Integer> goodTypeIds = CollectionCommonUtil.getFieldListByObjectList(shoppingCartListList,"getGoodsTypeId",Integer.class);
        ApiResponse<List<GoodsType>> apiResponse = goodsTypeServiceFacade.findGoodsTypeByIds(goodTypeIds);
        ApiResponseCheck.check(apiResponse);
        List<GoodsType> goodsTypeList = apiResponse.getBody();
        Map<Integer,GoodsType> goodsTypeMap = CollectionCommonUtil.toMapByList(goodsTypeList,"getId",Integer.class);
        ShoppingCartVo shoppingCartVo = new ShoppingCartVo();
        int goodsPieces = 0;
        double payAmount = 0;
        List<ShoppingCartListVo> shoppingCartListVos = baseMapper.mapAsList(shoppingCartListList, ShoppingCartListVo.class);
        for (ShoppingCartListVo shoppingCartListVo : shoppingCartListVos) {
            Integer num = shoppingCartListVo.getNum();
            goodsPieces += num;

            double numDouble = num.doubleValue();
            double priceDouble = shoppingCartListVo.getGoodsTypePrice();

            double total = CalculateUtil.mul(numDouble, priceDouble);
            shoppingCartListVo.setTotal(total);

            payAmount += total;
            int minNum = goodsTypeMap.get(shoppingCartListVo.getGoodsTypeId()).getMinNum();
            shoppingCartListVo.setCustom(minNum>0);
        }
        shoppingCartVo.setGoodsPieces(goodsPieces);
        shoppingCartVo.setPayAmount(payAmount);
        shoppingCartVo.setShoppingCartListList(shoppingCartListVos);
        resultData.setBody(shoppingCartVo);

        return resultData;
    }

    /**
     * 添加购物车
     *
     * @param goodsTypeId 商品类型ID
     * @param num         数量
     * @return
     */
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public ResultData add(@RequestParam Integer goodsTypeId, @RequestParam Integer num) {
        if(num.intValue()>9999 || num.intValue()<1) {
            return ResultData.failed("每笔订单中单品数量不超过9999!");
        }
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(Lists.newArrayList(goodsTypeId));
        ApiResponseCheck.check(apiResponse);
        List<ConfirmGoodsVo> body = apiResponse.getBody();
        if (CollectionUtils.isEmpty(body)) {
            return ResultData.failed("商品不存在！");
        }
        ConfirmGoodsVo confirmGoodsVo = body.get(0);
        if(confirmGoodsVo.getStatus() != GoodsConstant.Status.ON) {
            return ResultData.failed("商品已下架！");
        }
        if(confirmGoodsVo.getMinNum()>0 && num % confirmGoodsVo.getMinNum()!= 0){
            return ResultData.failed("定制商品数量有误！");
        }
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        // TODO 现在默认教材，将来扩展需要存其他类型的时候此处需要改，类别需要前端传过来。
        shoppingCartList.setUserId(UserHandleUtil.getUserId());
        shoppingCartList.setCategoryId(MallItemConstant.Category.JCZB.getId());
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        shoppingCartList.setNum(num);

        ApiResponse<Integer> addSCResponse = shoppingCartServiceFacade.addShoppingCart(shoppingCartList);
        ApiResponseCheck.check(addSCResponse);
        return ResultData.successed();
    }

    /**
     * 删除购物车
     *
     * @param goodsId     商品ID
     * @param goodsTypeId 商品类型ID
     * @return
     */
    @RequestMapping(value = "/del")
    public ResultData del(@RequestParam Integer goodsId, @RequestParam Integer goodsTypeId) {
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        shoppingCartList.setUserId(UserHandleUtil.getUserId());
        // TODO 现在默认教材，将来扩展需要存其他类型的时候此处需要改，类别需要前端传过来。
        Integer categoryId = MallItemConstant.Category.JCZB.getId();
        shoppingCartList.setCategoryId(categoryId);
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        ApiResponse<?> apiResponse = shoppingCartServiceFacade.delShoppingCart(shoppingCartList);
        if (apiResponse.isSuccess()){
            return ResultData.successed();
        }
        return ResultData.failed(apiResponse.getMessage());
    }

    /**
     * 修改购物车
     *
     * @param goodsId     商品ID
     * @param goodsTypeId 商品类型ID
     * @param num         数量
     * @return
     */
    @RequestMapping(value = "/mod")
    public ResultData mod(@RequestParam Integer goodsId, @RequestParam Integer goodsTypeId, @RequestParam Integer num) {
        if(num.intValue()>9999 || num.intValue()<1) {
            return ResultData.failed("每笔订单中单品数量不超过9999!");
        }
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        // TODO 现在默认教材，将来扩展需要存其他类型的时候此处需要改，类别需要前端传过来。
        shoppingCartList.setUserId(UserHandleUtil.getUserId());
        shoppingCartList.setCategoryId(MallItemConstant.Category.JCZB.getId());
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        shoppingCartList.setNum(num);
        ApiResponse<Integer> apiResponse = shoppingCartServiceFacade.updateShoppingCart(shoppingCartList);
        return ResultData.successed(apiResponse.getBody());
    }

}
