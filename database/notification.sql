-- Notification module DDL + seed (idempotent)
DROP TABLE IF EXISTS notification;
CREATE TABLE notification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
  emp_id BIGINT DEFAULT NULL COMMENT '接收员工ID (employee.id)',
  type VARCHAR(32) NOT NULL COMMENT '类型: repair_accept/repair_finish/fee_warning/change_room_result',
  title VARCHAR(128) NOT NULL COMMENT '标题',
  content VARCHAR(512) DEFAULT '' COMMENT '内容',
  biz_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
  is_read INT DEFAULT 0 COMMENT '0未读 1已读',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_emp (emp_id),
  KEY idx_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notification';

-- 超限预警阈值配置
INSERT INTO system_config (cfg_key, cfg_value, description) VALUES
  ('utility_threshold', '200', '水电费超限预警阈值（元）')
ON DUPLICATE KEY UPDATE cfg_value = VALUES(cfg_value);

-- 演示通知数据（员工1 张伟）
INSERT INTO notification (emp_id, type, title, content, biz_id, is_read) VALUES
  (1, 'repair_accept', '报修已接单', '您的报修「水龙头漏水」已由维修师傅接单，正在处理中。', 1, 0),
  (1, 'repair_finish', '维修已完成', '您的报修「空调不制冷」已处理完成，感谢您的反馈。', 2, 1),
  (1, 'fee_warning', '水电费超限提醒', '您所在房间 101 本期水电费合计 ¥268.00，已超过预警阈值 ¥200，请及时缴纳。', 1, 0),
  (1, 'change_room_result', '调宿申请已通过', '您提交的调宿申请已通过审批，请前往「入住信息」查看最新房间。', 1, 0);
