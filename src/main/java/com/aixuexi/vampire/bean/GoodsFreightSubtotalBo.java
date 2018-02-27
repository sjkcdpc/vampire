package com.aixuexi.vampire.bean;

/**
 * 运费计算过程中小计
 *
 * @author baopan
 * @createTime 20180208
 */
public class GoodsFreightSubtotalBo {
    Integer goodsPieces = 0; // 商品总件数
    Double weight = 0D; // 重量
    Double goodsAmount = 0D; // 总金额

    public Integer getGoodsPieces() {
        return goodsPieces;
    }

    public void setGoodsPieces(Integer goodsPieces) {
        this.goodsPieces = goodsPieces;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getGoodsAmount() {
        return goodsAmount;
    }

    public void setGoodsAmount(Double goodsAmount) {
        this.goodsAmount = goodsAmount;
    }
}
