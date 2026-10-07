package com.example.dorm.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/** Aggregated dashboard statistics. */
@Data
public class DashboardVO {
    private Long buildingCount;
    private Long roomCount;
    private Long roomEmptyCount;
    private Long employeeCount;
    private Long residentCount;
    private Long repairPendingCount;
    private Long feeUnpaidCount;
    private Long visitorTodayCount;
    private List<Map<String, Object>> occupancyByBuilding;
}
