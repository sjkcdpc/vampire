package com.aixuexi.vampire.controller;

import com.aixuexi.thor.redis.MyJedisService;
import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.CalculateUtil;
import com.aixuexi.vampire.util.Constants;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.facade.GoodsServiceFacade;
import com.gaosi.api.vulcan.facade.ShoppingCartServiceFacade;
import com.gaosi.api.vulcan.model.ShoppingCartList;
import com.gaosi.api.vulcan.vo.ConfirmGoodsVo;
import com.gaosi.api.vulcan.vo.ShoppingCartListVo;
import com.gaosi.api.vulcan.vo.ShoppingCartVo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by gaoxinzhong on 2017/5/24.
 */
@RestController
@RequestMapping(value = "/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartServiceFacade shoppingCartServiceFacade;

    @Autowired
    private GoodsServiceFacade goodsServiceFacade;

    @Autowired
    private MyJedisService myJedisService;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 购物车
     *
     * @return
     */
    @RequestMapping(value = "/list")
    public ResultData list() {
        ResultData resultData = new ResultData();
        List<ShoppingCartList> shoppingCartListList = shoppingCartServiceFacade.queryShoppingCartDetail(UserHandleUtil.getUserId());
        if (CollectionUtils.isNotEmpty(shoppingCartListList)) {
            ShoppingCartVo shoppingCartVo = new ShoppingCartVo();
            int goodsPieces = 0;
            double payAmount = 0;
            List<ShoppingCartListVo> shoppingCartListVos = baseMapper.mapAsList(shoppingCartListList, ShoppingCartListVo.class);
            for (ShoppingCartListVo shoppingCartListVo : shoppingCartListVos) {
                goodsPieces += shoppingCartListVo.getNum();
                double total = CalculateUtil.mul(shoppingCartListVo.getNum().doubleValue(), shoppingCartListVo.getGoodsTypePrice());
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
     * @param goodsTypeId 商品类型ID
     * @param num         数量
     * @return
     */
    @RequestMapping(value = "/add")
    public ResultData add(@RequestParam Integer goodsTypeId, @RequestParam Integer num) {
        ApiResponse<List<ConfirmGoodsVo>> apiResponse = goodsServiceFacade.queryGoodsInfo(Lists.newArrayList(goodsTypeId));
        if (CollectionUtils.isEmpty(apiResponse.getBody())) {
            return ResultData.failed("商品不存在！");
        }
        if(num.intValue()>5000 || num.intValue()<1) {
            return ResultData.failed("商品数量必须在1-5000之间！");
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
        String redisKey=Constants.PRE_SHOPPINGCART+UserHandleUtil.getUserId();
        try {
            boolean ret = myJedisService.setnx(redisKey, "", 60);
            if(!ret){
                return ResultData.failed("请勿操作过快！");
            }
            int flag = shoppingCartServiceFacade.addShoppingCart(shoppingCartList, UserHandleUtil.getUserId());
            if (flag == -2) {
                return ResultData.failed("购物车重复、请联系客服！");
            }
        }
        finally {
            myJedisService.del(redisKey);
        }
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
        int flag = shoppingCartServiceFacade.delShoppingCart(goodsId, goodsTypeId, UserHandleUtil.getUserId());
        return dealDelAndMod(flag);
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
        int flag = shoppingCartServiceFacade.modNumShoppingCart(goodsId, goodsTypeId, num, UserHandleUtil.getUserId());
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
