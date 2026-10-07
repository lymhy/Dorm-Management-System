package com.example.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dorm.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/** MyBatis-Plus mapper for Announcement. */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
