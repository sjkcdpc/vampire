package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.ShoppingCartServiceFacade;
import com.gaosi.api.vulcan.model.ShoppingCartList;
import com.gaosi.api.vulcan.vo.ShoppingCartListVo;
import com.gaosi.api.vulcan.vo.ShoppingCartVo;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoxinzhong on 2017/5/24.
 */
@RestController
@RequestMapping(value = "/shoppingCart")
public class ShoppingCartController {
    private final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);
    @Resource
    private ShoppingCartServiceFacade shoppingCartServiceFacade;

    /**
     * 购物车
     *
     * @return
     */
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    public ResultData list() {
        ResultData resultData = new ResultData();

        Integer userId = UserHandleUtil.getUserId();
        ApiResponse<List<ShoppingCartListVo>> listApiResponse = shoppingCartServiceFacade.queryShoppingCartDetail(userId);
        List<ShoppingCartListVo> shoppingCartListVos = listApiResponse.getBody();
        if (CollectionUtils.isEmpty(shoppingCartListVos)) {
            return resultData;
        }
        ShoppingCartVo shoppingCartVo = new ShoppingCartVo();
        int goodsPieces = 0;
        double payAmount = 0;
        for (ShoppingCartListVo shoppingCartListVo : shoppingCartListVos) {
            goodsPieces += shoppingCartListVo.getNum();
            payAmount += shoppingCartListVo.getTotal();
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
        if(num>9999 || num<1) {
            return ResultData.failed("每笔订单中单品数量不超过9999!");
        }
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        // TODO 现在默认教材，将来扩展需要存其他类型的时候此处需要改，类别需要前端传过来。
        shoppingCartList.setUserId(UserHandleUtil.getUserId());
        shoppingCartList.setCategoryId(MallItemConstant.Category.JCZB.getId());
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        shoppingCartList.setNum(num);

        shoppingCartServiceFacade.addShoppingCart(shoppingCartList);
        return ResultData.successed();
    }

    /**
     * 删除购物车
     *
     * @param goodsTypeId 商品类型ID
     * @return
     */
    @RequestMapping(value = "/del")
    public ResultData del(@RequestParam Integer goodsTypeId) {
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
     * @param goodsTypeId 商品类型ID
     * @param num         数量
     * @return
     */
    @RequestMapping(value = "/mod")
    public ResultData mod(@RequestParam Integer goodsTypeId, @RequestParam Integer num) {
        if (num > 9999 || num < 1) {
            return ResultData.failed("每笔订单中单品数量不超过9999!");
        }
        ShoppingCartList shoppingCartList = new ShoppingCartList();
        // TODO 现在默认教材，将来扩展需要存其他类型的时候此处需要改，类别需要前端传过来。
        shoppingCartList.setUserId(UserHandleUtil.getUserId());
        shoppingCartList.setCategoryId(MallItemConstant.Category.JCZB.getId());
        shoppingCartList.setGoodsTypeId(goodsTypeId);
        shoppingCartList.setNum(num);
        ApiResponse<Double> apiResponse = shoppingCartServiceFacade.updateShoppingCart(shoppingCartList);
        Map<String, Object> map = new HashMap<>();
        map.put("goodsTypeId", goodsTypeId);
        map.put("price", apiResponse.getBody());
        return ResultData.successed(map);
    }

}
