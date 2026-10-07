package com.example.dorm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dorm.entity.Fee;
import com.example.dorm.entity.PayOrder;

import java.util.List;

/** 在线支付业务：下单、确认、关闭、查询 */
public interface PayOrderService extends IService<PayOrder> {

    /**
     * 为账单创建支付订单（同一账单同时只允许一笔待支付订单）。
     * @param empId    缴费员工（可为空 = 管理员代付场景）
     * @param operator 发起人用户名
     * @return 收银参数（渠道返回值，模拟通道为收银台地址）
     */
    PayOrder createOrder(Long feeId, Long empId, String channel, String operator);

    /** 确认支付（模拟渠道回调等价物）。幂等：重复确认返回已有成功订单。 */
    PayOrder confirmPaid(String orderNo, String outTradeNo, String callbackPayload);

    /** 关闭未支付订单（用户取消或超时） */
    boolean closeOrder(String orderNo, String reason);

    /** 按订单号查询 */
    PayOrder getByOrderNo(String orderNo);

    /** 员工的支付订单分页 */
    List<PayOrder> myOrders(Long empId, int limit);

    /** 订单关闭时校验账单是否仍可支付 */
    Fee loadPayableFeeOrThrow(Long feeId, PayOrder order);
}