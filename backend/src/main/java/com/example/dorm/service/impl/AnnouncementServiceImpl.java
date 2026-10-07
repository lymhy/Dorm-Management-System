package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Announcement;
import com.example.dorm.mapper.AnnouncementMapper;
import com.example.dorm.service.AnnouncementService;
import org.springframework.stereotype.Service;

/** Service implementation for Announcement. */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {
}
