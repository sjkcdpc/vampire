package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.sms_mail.SMSConstant;
import com.aixuexi.transformers.mq.ONSMQProducer;
import com.aixuexi.transformers.msg.SmsSend;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.User;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.model.ItemOrder;
import com.gaosi.api.revolver.model.ItemOrderDetail;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.revolver.vo.ItemOrderDetailVo;
import com.gaosi.api.revolver.vo.ItemOrderVo;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.model.MallItem;
import com.gaosi.api.vulcan.model.MallSku;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.aixuexi.vampire.util.Constants.ORDERDETAIL_NAME_DIV;

/**
 * @Description:商品订单管理，供controller使用
 * @Author: liuxinyun
 * @Date: 2017/8/10 14:20
 */
@Service("itemOrderManager")
public class ItemOrderManager {

    private static final Logger logger = LoggerFactory.getLogger(ItemOrderManager.class);

    @Resource
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Autowired(required = false)
    private ONSMQProducer mqProducer;

    @Value("${order_update_fail_receive_phone}")
    private String phoneStr;

    @Resource
    private FinancialAccountManager financialAccountManager;

    @Resource
    private UserService userService;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 虚拟商品生成订单对象
     * @param mallItem
     * @param mallSku
     * @param num
     * @return
     */
    public ItemOrderVo generateItemOrderVo(MallItem mallItem, MallSku mallSku, Integer num) {
        ItemOrderVo itemOrderVo = new ItemOrderVo();
        itemOrderVo.setInstitutionId(UserHandleUtil.getInsId());
        itemOrderVo.setUserId(UserHandleUtil.getUserId());
        //虚拟商品没有收货人，默认收货人为当前用户,收货人电话为当前用户的电话
        itemOrderVo.setConsigneeName(UserSessionHandler.getUsername());
        User user = userService.getUserById(UserHandleUtil.getUserId());
        itemOrderVo.setConsigneePhone(user.getTelephone());
        //只要提交订单就是待支付，确认支付后再更改状态
        itemOrderVo.setStatus(OrderConstant.OrderStatus.NO_PAY.getValue());
        itemOrderVo.setCategoryId(mallItem.getCategoryId());
        //订单详情
        List<ItemOrderDetailVo> itemOrderDetailVos = new ArrayList<>();
        ItemOrderDetailVo itemOrderDetailVo = new ItemOrderDetailVo();
        itemOrderDetailVo.setItemId(mallItem.getId());
        itemOrderDetailVo.setItemName(mallItem.getName());
        if (StringUtils.isNotBlank(mallSku.getName())) {
            itemOrderDetailVo.setItemName(mallItem.getName() + ORDERDETAIL_NAME_DIV + mallSku.getName());
        }
        if (mallSku.getId() != null) {
            itemOrderDetailVo.setMallSkuId(mallSku.getId());
        }
        itemOrderDetailVo.setItemPrice(mallSku.getPrice());
        itemOrderDetailVo.setItemCount(num);
        itemOrderDetailVo.setDiscount(0D);
        if (mallSku.getOriginalPrice() != null && mallSku.getOriginalPrice() != 0) {
            itemOrderDetailVo.setDiscount(AmountUtil.subtract(mallSku.getOriginalPrice(), mallSku.getPrice()));
        }
        itemOrderDetailVo.setBusinessId(MallItemConstant.Category.getCode(mallItem.getCategoryId()));
        itemOrderDetailVos.add(itemOrderDetailVo);
        itemOrderVo.setItemOrderDetails(itemOrderDetailVos);
        return itemOrderVo;
    }

    /**
     * 虚拟商品提交订单
     * @param itemOrderVo
     * @return
     */
    public String submit(ItemOrderVo itemOrderVo) {
        //查询当前机构账号余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(itemOrderVo.getInstitutionId());
        List<ItemOrderDetailVo> itemOrderDetailVos = itemOrderVo.getItemOrderDetails();
        // 计算订单总金额
        Double consumeCount = 0D;
        for (ItemOrderDetailVo itemOrderDetailVo : itemOrderDetailVos) {
            consumeCount = AmountUtil.multiply(itemOrderDetailVo.getItemPrice(),itemOrderDetailVo.getItemCount());
        }
        Double totalCount = AmountUtil.multiply(consumeCount , 10000D);
        // 检查账户余额
        financialAccountManager.checkRemainMoney(rr,totalCount.longValue());
        ItemOrder itemOrder = baseMapper.map(itemOrderVo,ItemOrder.class);
        itemOrder.setConsumeCount(consumeCount);
        // 订单详情
        List<ItemOrderDetail> itemOrderDetails = baseMapper.mapAsList(itemOrderDetailVos,ItemOrderDetail.class);
        // 创建订单
        ApiResponse<String> apiResponse = itemOrderServiceFacade.createOrder(itemOrder, itemOrderDetails);
        ApiResponseCheck.check(apiResponse);
        return apiResponse.getBody();
    }



    /**
     * 付款后更新订单状态
     *
     * @param orderId
     */
    public void updateOrderStatus(String orderId) {
        boolean flag = true;
        try {
            int retryNum = 0;
            while (retryNum < 3) {//重试三次
                ApiResponse<?> apiResponse = itemOrderServiceFacade.updateOrderStatus(orderId, OrderConstant.OrderStatus.COMPLETED.getValue());
                if (apiResponse == null || apiResponse.isNotSuccess()) {
                    //更新状态失败，重试次数累加。
                    retryNum++;
                    //等待100毫秒后重试
                    Thread.sleep(100);
                } else {
                    flag = false;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("orderId=[{}] pay success, but updateStatus failed for {}.", orderId, e);
        } finally {
            //重试三次更新状态均失败，打印错误日志。(发短信通知)
            if (flag) {
                logger.error("orderId=[{}] pay success, but updateStatus failed.", orderId);
                String[] phones = phoneStr.split(",");
                SmsSend.SmsSendObject.Builder builder = SmsSend.SmsSendObject.newBuilder();
                builder.setSignName(SMSConstant.SIGN_AIXUEXI)
                        .putParam("orderId", orderId)
                        .setTemplateCode(SMSConstant.TEMPLATE_CODE_ORDER_UPDATE_FAIL_NOTIFY)
                        .addAllPhones(Arrays.asList(phones))
                        .setBusinessType(SMSConstant.BUSINESS_TYPE_ORDER_UPDATE_FAIL_NOTIFY);
                mqProducer.send(builder);
            }
        }
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderId
     * @return
     */
    public ItemOrder getOrderByOrderId(String orderId) {
        ApiResponse<ItemOrderVo> itemOrderResponse = itemOrderServiceFacade.getOrderByOrderId(orderId);
        ApiResponseCheck.check(itemOrderResponse);
        ItemOrderVo itemOrderVo = itemOrderResponse.getBody();
        if (itemOrderVo == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "订单:" + orderId + "不存在");
        }
        return itemOrderVo;
    }

}
