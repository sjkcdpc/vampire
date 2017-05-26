package com.aixuexi.vampire.controller;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.except.IllegalArgException;
import com.aixuexi.thor.response.ResultData;
import com.gaosi.api.independenceDay.entity.ShoppingCartList;
import com.gaosi.api.independenceDay.service.ShoppingCartService;
import com.gaosi.api.independenceDay.vo.ShoppingCartListVo;
import com.gaosi.api.revolver.GoodsService;
import com.gaosi.api.revolver.vo.ConfirmGoodsVo;
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
    private ShoppingCartService shoppingCartService;
    @Autowired
    private GoodsService goodsService;


    /**
     * 购物车
     *
     * @param userId 用户ID
     * @return
     */
    @RequestMapping(value = "/list")
    public ResultData list(@RequestParam Integer userId) {
        ResultData resultData = new ResultData();
        List<ShoppingCartList> shoppingCartListList = shoppingCartService.queryShoppingCartDetail(userId);
        if (CollectionUtils.isNotEmpty(shoppingCartListList)) {
            ShoppingCartListVo shoppingCartListVo = new ShoppingCartListVo();
            int goodsPieces = 0;
            double payAmount = 0;
            for (ShoppingCartList shoppingCartList : shoppingCartListList) {
                goodsPieces += shoppingCartList.getNum();
                payAmount += (shoppingCartList.getNum() * shoppingCartList.getGoodsTypePrice());
            }
            shoppingCartListVo.setGoodsPieces(goodsPieces);
            shoppingCartListVo.setPayAmount(payAmount);
            shoppingCartListVo.setShoppingCartListList(shoppingCartListList);
            resultData.setBody(shoppingCartListVo);
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
        List<ConfirmGoodsVo> goodsVos = goodsService.queryGoodsInfo(Lists.newArrayList(goodsTypeId));
        if (CollectionUtils.isEmpty(goodsVos)) {
            throw new IllegalArgException(ExceptionCode.UNKNOWN, "商品不存在");
        }
        ConfirmGoodsVo confirmGoodsVo = goodsVos.get(0);
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        shoppingCartList.setGoodsId(confirmGoodsVo.getGoodsId());
        shoppingCartList.setGoodsName(confirmGoodsVo.getGoodsName());
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        shoppingCartList.setGoodsTypeName(confirmGoodsVo.getGoodsTypeName());
        shoppingCartList.setGoodsTypePrice(confirmGoodsVo.getPrice());
        shoppingCartList.setNum(num);
        shoppingCartList.setWeight(confirmGoodsVo.getWeight());
        return shoppingCartService.addShoppingCart(shoppingCartList, userId);
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
        return shoppingCartService.delShoppingCart(goodsId, goodsTypeId, userId);
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
        return shoppingCartService.modNumShoppingCart(goodsId, goodsTypeId, num, userId);
    }


}
