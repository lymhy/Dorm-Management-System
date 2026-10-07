package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dorm.dto.DashboardVO;
import com.example.dorm.entity.Building;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.Fee;
import com.example.dorm.entity.Repair;
import com.example.dorm.entity.Room;
import com.example.dorm.entity.Visitor;
import com.example.dorm.mapper.BuildingMapper;
import com.example.dorm.mapper.EmployeeMapper;
import com.example.dorm.mapper.FeeMapper;
import com.example.dorm.mapper.RepairMapper;
import com.example.dorm.mapper.RoomMapper;
import com.example.dorm.mapper.VisitorMapper;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/** Dashboard aggregated statistics. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource private BuildingMapper buildingMapper;
    @Resource private RoomMapper roomMapper;
    @Resource private EmployeeMapper employeeMapper;
    @Resource private RepairMapper repairMapper;
    @Resource private FeeMapper feeMapper;
    @Resource private VisitorMapper visitorMapper;

    @GetMapping("/stats")
    public Result<DashboardVO> stats() {
        DashboardVO vo = new DashboardVO();
        vo.setBuildingCount(buildingMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setRoomCount(roomMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setRoomEmptyCount(roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStatus, 0)));
        vo.setEmployeeCount(employeeMapper.selectCount(new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1)));
        vo.setResidentCount(roomMapper.selectCount(new LambdaQueryWrapper<Room>().gt(Room::getOccupied, 0)));
        vo.setRepairPendingCount(repairMapper.selectCount(new LambdaQueryWrapper<Repair>().lt(Repair::getStatus, 2)));
        vo.setFeeUnpaidCount(feeMapper.selectCount(new LambdaQueryWrapper<Fee>().eq(Fee::getPaid, 0)));
        vo.setVisitorTodayCount(visitorMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setOccupancyByBuilding(buildingMapper.selectRoomCountByBuilding());
        return Result.success(vo);
    }
}
