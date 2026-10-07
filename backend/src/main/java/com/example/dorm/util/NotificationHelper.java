package com.example.dorm.util;

import com.example.dorm.entity.Notification;
import com.example.dorm.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * Helper for pushing messages/notifications to employees.
 * Type constants:
 *   repair_accept      报修接单通知
 *   repair_finish      维修完成提醒
 *   fee_warning        水电超限预警
 *   change_room_result 换寝审批结果
 */
@Component
public class NotificationHelper {

    @Resource
    private NotificationService notificationService;

    public void send(Long empId, String type, String title, String content, Long bizId) {
        if (empId == null) return;
        Notification n = new Notification();
        n.setEmpId(empId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setBizId(bizId);
        n.setIsRead(0);
        n.setDeleted(0);
        n.setTenantId(0L);
        notificationService.save(n);
    }
}
