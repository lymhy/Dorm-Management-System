package com.example.dorm.pay;

import com.example.dorm.entity.PayOrder;
import org.springframework.stereotype.Component;

/**
 * 模拟支付通道（开发/演示用）。
 * createPayment 返回一个模拟收银台地址；真实渠道接入时替换为
 * 微信 Native 下单 / 支付宝 precreate 等真实实现即可。
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    public static final String CHANNEL = "mock";

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public String createPayment(PayOrder order) {
        // 模拟收银台地址：携带订单号与金额，收银台页确认后调用 confirm 接口完成支付
        return "/pay/cashier?orderNo=" + order.getOrderNo()
                + "&amount=" + order.getAmount()
                + "&subject=" + java.net.URLEncoder.encode(
                        order.getSubject() == null ? "宿舍缴费" : order.getSubject(),
                        java.nio.charset.StandardCharsets.UTF_8);
    }
}