package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.facade.GoodsServiceFacade;
import com.gaosi.api.revolver.facade.ShoppingCartFacade;
import com.gaosi.api.revolver.model.ShoppingCartList;
import com.gaosi.api.revolver.vo.ConfirmGoodsVo;
import com.gaosi.api.revolver.vo.ShoppingCartListVo;
import com.gaosi.api.revolver.vo.ShoppingCartVo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by gaoxinzhong on 2017/5/24.
 */
@RestController
@RequestMapping(value = "/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartFacade shoppingCartFacade;

    @Autowired
    private GoodsServiceFacade goodsServiceFacade;

    /**
     * 购物车
     *
     * @param userId 用户ID
     * @return
     */
    @RequestMapping(value = "/list")
    public ResultData list(@RequestParam Integer userId) {
        ResultData resultData = new ResultData();
        List<ShoppingCartList> shoppingCartListList = shoppingCartFacade.queryShoppingCartDetail(userId);
        if (CollectionUtils.isNotEmpty(shoppingCartListList)) {
            ShoppingCartVo shoppingCartVo = new ShoppingCartVo();
            int goodsPieces = 0;
            double payAmount = 0;
            String jsonString = JSONObject.toJSONString(shoppingCartListList);
            List<ShoppingCartListVo> shoppingCartListVos = JSONArray.parseArray(jsonString, ShoppingCartListVo.class);
            for (ShoppingCartListVo shoppingCartListVo : shoppingCartListVos) {
                goodsPieces += shoppingCartListVo.getNum();
                double total = shoppingCartListVo.getNum() * shoppingCartListVo.getGoodsTypePrice();
                shoppingCartListVo.setTotal(total);
                payAmount += total;
            }
            shoppingCartVo.setGoodsPieces(goodsPieces);
            shoppingCartVo.setPayAmount(payAmount);
            shoppingCartVo.setShoppingCartListList(shoppingCartListVos);
            resultData.setBody(shoppingCartVo);
        }
        return resultData;
    }

    /**
     * 添加购物车
     *
     * @param userId      用户ID
     * @param goodsTypeId 商品类型ID
     * @param num         数量
     * @return
     */
    @RequestMapping(value = "/add")
    public ResultData add(@RequestParam Integer userId, @RequestParam Integer goodsTypeId, @RequestParam Integer num) {
        ResultData resultData = new ResultData();
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(Lists.newArrayList(goodsTypeId));
        if (CollectionUtils.isEmpty(apiResponse.getBody())) {
            resultData.setStatus(ResultData.STATUS_ERROR);
            resultData.setErrorMessage("商品不存在！");
            return resultData;
        }
        ConfirmGoodsVo confirmGoodsVo = apiResponse.getBody().get(0);
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        shoppingCartList.setGoodsId(confirmGoodsVo.getGoodsId());
        shoppingCartList.setGoodsName(confirmGoodsVo.getGoodsName());
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        shoppingCartList.setGoodsTypeName(confirmGoodsVo.getGoodsTypeName());
        shoppingCartList.setGoodsTypePrice(confirmGoodsVo.getPrice());
        shoppingCartList.setNum(num);
        shoppingCartList.setWeight(confirmGoodsVo.getWeight());
        int flag = shoppingCartFacade.addShoppingCart(shoppingCartList, userId);
        if (flag == -2) {
            resultData.setStatus(ResultData.STATUS_ERROR);
            resultData.setErrorMessage("购物车重复、请联系客服！");
            return resultData;
        }
        resultData.setBody(flag);
        return resultData;
    }

    /**
     * 删除购物车
     *
     * @param userId      用户ID
     * @param goodsId     商品ID
     * @param goodsTypeId 商品类型ID
     * @return
     */
    @RequestMapping(value = "/del")
    public ResultData del(@RequestParam Integer userId, @RequestParam Integer goodsId, @RequestParam Integer goodsTypeId) {
        int flag = shoppingCartFacade.delShoppingCart(goodsId, goodsTypeId, userId);
        return dealDelAndMod(flag);
    }

    /**
     * 修改购物车
     *
     * @param userId      用户ID
     * @param goodsId     商品ID
     * @param goodsTypeId 商品类型ID
     * @param num         数量
     * @return
     */
    @RequestMapping(value = "/mod")
    public ResultData mod(@RequestParam Integer userId, @RequestParam Integer goodsId, @RequestParam Integer goodsTypeId, @RequestParam Integer num) {
        int flag = shoppingCartFacade.modNumShoppingCart(goodsId, goodsTypeId, num, userId);
        return dealDelAndMod(flag);
    }

    private ResultData dealDelAndMod(int flag) {
        ResultData resultData = new ResultData();
        if (flag == -1) {
            resultData.setStatus(ResultData.STATUS_ERROR);
            resultData.setErrorMessage("购物车不存在数据、请刷新再试！");
        } else if (flag == -2) {
            resultData.setStatus(ResultData.STATUS_ERROR);
            resultData.setErrorMessage("购物车重复、请联系客服！");
        } else {
            resultData.setBody(flag);
        }
        return resultData;
    }
}
