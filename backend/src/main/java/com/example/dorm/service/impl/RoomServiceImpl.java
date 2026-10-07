package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Room;
import com.example.dorm.mapper.RoomMapper;
import com.example.dorm.service.RoomService;
import org.springframework.stereotype.Service;

/** Service implementation for Room. */
@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {
}
