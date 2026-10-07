package com.example.dorm.pay;

import com.example.dorm.entity.PayOrder;

/**
 * 可插拔支付网关抽象。
 * 新增真实渠道（微信/支付宝等）时实现本接口并注册为 Spring Bean 即可，
 * PayOrderService 按订单上的 channel 字段路由到对应实现。
 */
public interface PaymentGateway {

    /** 渠道标识，对应 pay_order.channel */
    String channel();

    /**
     * 渠道下单。返回透传给前端的收银参数（如二维码内容、跳转地址等）。
     * 实现方应只做参数组装，不落库 —— 订单状态由 PayOrderService 统一管理。
     */
    String createPayment(PayOrder order);

    /** 渠道方交易号格式校验（用于回调幂等） */
    default boolean supports(String outTradeNo) {
        return outTradeNo != null && !outTradeNo.isBlank();
    }
}