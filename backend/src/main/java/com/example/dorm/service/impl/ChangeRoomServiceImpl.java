package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.ChangeRoom;
import com.example.dorm.mapper.ChangeRoomMapper;
import com.example.dorm.service.ChangeRoomService;
import org.springframework.stereotype.Service;

/** Service implementation for ChangeRoom. */
@Service
public class ChangeRoomServiceImpl extends ServiceImpl<ChangeRoomMapper, ChangeRoom> implements ChangeRoomService {
}
