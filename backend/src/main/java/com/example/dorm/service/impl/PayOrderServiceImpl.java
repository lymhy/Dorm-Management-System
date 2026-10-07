package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dorm.entity.Allocation;
import com.example.dorm.entity.Fee;
import com.example.dorm.entity.FeePayment;
import com.example.dorm.entity.PayOrder;
import com.example.dorm.mapper.AllocationMapper;
import com.example.dorm.mapper.FeeMapper;
import com.example.dorm.mapper.FeePaymentMapper;
import com.example.dorm.mapper.PayOrderMapper;
import com.example.dorm.pay.PaymentGateway;
import com.example.dorm.service.NotificationService;
import com.example.dorm.service.PayOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.util.NotificationHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付订单核心逻辑。
 * 状态机: (创建) -> PENDING -> SUCCESS | CLOSED
 * 幂等约束: 订单号唯一；confirmPaid 重复回调直接返回既有成功订单；
 *           同一账单同一时刻最多一笔 PENDING 订单。
 */
@Service
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements PayOrderService {

    @Resource
    private FeeMapper feeMapper;
    @Resource
    private FeePaymentMapper feePaymentMapper;
    @Resource
    private AllocationMapper allocationMapper;
    @Resource
    private NotificationHelper notificationHelper;
    @Resource
    private ApplicationContext applicationContext;

    private static final long EXPIRE_MINUTES = 30;

    @Override
    @Transactional
    public PayOrder createOrder(Long feeId, Long empId, String channel, String operator) {
        Fee fee = feeMapper.selectById(feeId);
        if (fee == null || (fee.getDeleted() != null && fee.getDeleted() == 1)) {
            throw new IllegalArgumentException("账单不存在");
        }
        if (fee.getPaid() != null && fee.getPaid() == 1) {
            throw new IllegalArgumentException("该账单已缴纳，无需重复支付");
        }

        // 同一账单只保留一笔待支付订单：过期 PENDING 自动关闭，有效 PENDING 直接复用
        List<PayOrder> pendings = list(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getFeeId, feeId)
                .eq(PayOrder::getStatus, "PENDING"));
        for (PayOrder p : pendings) {
            if (p.getExpireTime() != null && p.getExpireTime().isBefore(LocalDateTime.now())) {
                p.setStatus("CLOSED");
                p.setRemark("超时自动关闭");
                updateById(p);
            } else {
                return p;
            }
        }

        PayOrder order = new PayOrder();
        order.setOrderNo(genOrderNo());
        order.setFeeId(fee.getId());
        order.setRoomId(fee.getRoomId());
        order.setEmployeeId(empId);
        order.setAmount(fee.getTotal() == null ? BigDecimal.ZERO : fee.getTotal());
        order.setMonth(fee.getMonth());
        order.setChannel(channel == null || channel.isBlank() ? "mock" : channel);
        order.setStatus("PENDING");
        order.setSubject("宿舍水电费 " + (fee.getMonth() == null ? "" : fee.getMonth()));
        order.setExpireTime(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        order.setCreator(operator == null ? "system" : operator);
        order.setDeleted(0);
        order.setTenantId(0L);
        save(order);

        // 渠道下单参数（模拟通道返回收银台地址；真实渠道在此返回 prepay/二维码内容）
        PaymentGateway gateway = resolveGateway(order.getChannel());
        String payParams = gateway.createPayment(order);
        order.setRemark(payParams);
        updateById(order);
        return order;
    }

    @Override
    @Transactional
    public PayOrder confirmPaid(String orderNo, String outTradeNo, String callbackPayload) {
        PayOrder order = getByOrderNo(orderNo);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if ("SUCCESS".equals(order.getStatus())) {
            return order; // 幂等：渠道重复回调
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("订单已关闭，无法确认支付");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setStatus("CLOSED");
            order.setRemark("超时后收到支付确认，已关闭");
            updateById(order);
            throw new IllegalStateException("订单已超时关闭");
        }

        order.setStatus("SUCCESS");
        order.setOutTradeNo(outTradeNo);
        order.setPaidTime(LocalDateTime.now());
        order.setCallbackPayload(callbackPayload);
        updateById(order);

        settleFee(order);
        return order;
    }

    /** 账单核销：paid=1 + 写在线支付流水 + 给房间在住员工发通知 */
    private void settleFee(PayOrder order) {
        Fee fee = feeMapper.selectById(order.getFeeId());
        if (fee == null) return;
        if (fee.getPaid() == null || fee.getPaid() != 1) {
            Fee upd = new Fee();
            upd.setId(fee.getId());
            upd.setPaid(1);
            feeMapper.updateById(upd);
        }

        Long count = feePaymentMapper.selectCount(new LambdaQueryWrapper<FeePayment>()
                .eq(FeePayment::getFeeId, fee.getId()));
        if (count == null || count == 0) {
            FeePayment p = new FeePayment();
            p.setFeeId(fee.getId());
            p.setRoomId(order.getRoomId());
            p.setEmployeeId(order.getEmployeeId());
            p.setAmount(order.getAmount());
            p.setMonth(order.getMonth());
            p.setPayTime(order.getPaidTime());
            p.setPayMethod("在线支付");
            p.setOperator(order.getCreator());
            p.setRemark("在线支付订单 " + order.getOrderNo());
            feePaymentMapper.insert(p);
        }

        if (order.getRoomId() != null) {
            List<Allocation> allocs = allocationMapper.selectList(new LambdaQueryWrapper<Allocation>()
                    .eq(Allocation::getRoomId, order.getRoomId())
                    .eq(Allocation::getStatus, 1));
            for (Allocation a : allocs) {
                notificationHelper.send(a.getEmployeeId(), "fee_paid", "缴费成功通知",
                        "您所在房间 " + (order.getMonth() == null ? "本期" : order.getMonth())
                                + " 水电费 \u00a5" + order.getAmount() + " 已通过在线支付缴纳成功。",
                        fee.getId());
            }
        }
    }

    @Override
    @Transactional
    public boolean closeOrder(String orderNo, String reason) {
        PayOrder order = getByOrderNo(orderNo);
        if (order == null) return false;
        if ("SUCCESS".equals(order.getStatus())) return false;
        order.setStatus("CLOSED");
        order.setRemark(reason == null ? "用户关闭" : reason);
        return updateById(order);
    }

    @Override
    public PayOrder getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, orderNo));
    }

    @Override
    public List<PayOrder> myOrders(Long empId, int limit) {
        return list(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getEmployeeId, empId)
                .orderByDesc(PayOrder::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    @Override
    public Fee loadPayableFeeOrThrow(Long feeId, PayOrder order) {
        Fee fee = feeMapper.selectById(feeId);
        if (fee == null) throw new IllegalArgumentException("账单不存在");
        return fee;
    }

    private PaymentGateway resolveGateway(String channel) {
        Map<String, PaymentGateway> gateways = applicationContext.getBeansOfType(PaymentGateway.class);
        for (PaymentGateway g : gateways.values()) {
            if (g.channel().equals(channel)) return g;
        }
        return gateways.values().iterator().next();
    }

    /** 订单号: PO + yyyyMMddHHmmss + 6位随机 */
    private String genOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PO" + ts + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}