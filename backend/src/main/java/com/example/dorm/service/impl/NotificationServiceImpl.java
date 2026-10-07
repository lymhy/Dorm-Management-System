package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Notification;
import com.example.dorm.mapper.NotificationMapper;
import com.example.dorm.service.NotificationService;
import org.springframework.stereotype.Service;

/** Service implementation for Notification. */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
}
