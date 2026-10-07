package com.example.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dorm.entity.Building;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** MyBatis-Plus mapper for Building. */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {

    @Select("SELECT b.name AS building_name, COUNT(r.id) AS room_cnt " +
            "FROM building b " +
            "LEFT JOIN room r ON r.building_id = b.id AND r.deleted = 0 " +
            "WHERE b.deleted = 0 " +
            "GROUP BY b.id, b.name")
    List<Map<String, Object>> selectRoomCountByBuilding();
}
