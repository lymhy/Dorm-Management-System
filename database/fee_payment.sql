-- 缴费流水表
CREATE TABLE IF NOT EXISTS fee_payment (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  fee_id BIGINT DEFAULT NULL COMMENT '关联账单ID',
  room_id BIGINT DEFAULT NULL COMMENT '房间ID',
  employee_id BIGINT DEFAULT NULL COMMENT '缴费员工ID',
  amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '缴费金额',
  month VARCHAR(20) DEFAULT NULL COMMENT '账单月份',
  pay_time DATETIME DEFAULT NULL COMMENT '缴费时间',
  pay_method VARCHAR(20) DEFAULT '线下缴纳' COMMENT '缴费方式',
  operator VARCHAR(50) DEFAULT NULL COMMENT '经办人',
  remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
  creator VARCHAR(50) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  updater VARCHAR(50) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
  tenant_id BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_fee_id (fee_id),
  KEY idx_room_id (room_id),
  KEY idx_pay_time (pay_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缴费流水';

-- 为已有"已缴"账单补录一条流水（幂等）
INSERT INTO fee_payment (fee_id, room_id, amount, month, pay_time, pay_method, operator, remark, create_time, deleted)
SELECT f.id, f.room_id, f.total, f.month, NOW(), '线下缴纳', '系统管理员', '历史缴费补录', NOW(), 0
FROM fee f
WHERE f.paid = 1 AND f.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM fee_payment p WHERE p.fee_id = f.id AND p.deleted = 0);
