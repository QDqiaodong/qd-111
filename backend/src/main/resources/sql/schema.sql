SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

CREATE DATABASE IF NOT EXISTS instrument_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE instrument_manager;

DROP TABLE IF EXISTS accessory_group;
CREATE TABLE accessory_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '分组名称',
    sort_order INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配件分组';

DROP TABLE IF EXISTS accessory;
CREATE TABLE accessory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '配件名称',
    type_code VARCHAR(50) NOT NULL COMMENT '配件类型编码',
    type_name VARCHAR(50) DEFAULT NULL COMMENT '配件类型名称',
    specification VARCHAR(500) DEFAULT NULL COMMENT '规格参数',
    instrument VARCHAR(50) NOT NULL COMMENT '适配乐器编码',
    instrument_name VARCHAR(50) DEFAULT NULL COMMENT '适配乐器名称',
    group_id BIGINT DEFAULT NULL COMMENT '所属分组ID',
    group_name VARCHAR(50) DEFAULT NULL COMMENT '所属分组名称',
    brand_model VARCHAR(100) DEFAULT NULL COMMENT '品牌型号',
    standard_cycle INT DEFAULT 90 COMMENT '标准更换周期(天)',
    purchase_date DATE DEFAULT NULL COMMENT '购入时间',
    worn_status VARCHAR(20) DEFAULT 'good' COMMENT '损耗状态 good-完好 slight-轻微磨损 severe-严重损耗 broken-已损坏',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '配图URL',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_group_id (group_id),
    KEY idx_type_code (type_code),
    KEY idx_worn_status (worn_status),
    KEY idx_purchase_date (purchase_date),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配件耗材';

DROP TABLE IF EXISTS replacement_record;
CREATE TABLE replacement_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    accessory_id BIGINT NOT NULL COMMENT '配件ID',
    accessory_name VARCHAR(100) DEFAULT NULL COMMENT '配件名称',
    specification VARCHAR(500) DEFAULT NULL COMMENT '规格',
    instrument_name VARCHAR(50) DEFAULT NULL COMMENT '适配乐器',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '配图',
    replace_date DATE NOT NULL COMMENT '更换日期',
    standard_cycle INT DEFAULT 90 COMMENT '标准更换周期(天)',
    usage_days INT DEFAULT 0 COMMENT '实际使用天数',
    operator VARCHAR(50) DEFAULT NULL COMMENT '操作人',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_accessory_id (accessory_id),
    KEY idx_replace_date (replace_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更换记录'
;

INSERT INTO accessory_group (name, sort_order, description) VALUES
('弹奏配件', 1, '直接参与演奏发声的配件，如琴弦、琴弓、拨片等'),
('辅助工具', 2, '演奏过程中使用的辅助工具，如变调夹、背带、调音器等'),
('养护耗材', 3, '乐器清洁、保养使用的消耗品，如松香、清洁剂、擦布等');

INSERT INTO accessory (name, type_code, type_name, specification, instrument, instrument_name, group_id, group_name, brand_model, standard_cycle, purchase_date, worn_status, remark) VALUES
('木吉他琴弦', 'string', '琴弦', '012-053 磷铜覆膜', 'guitar-acoustic', '木吉他', 1, '弹奏配件', 'Elixir Nanoweb', 90, '2026-04-01', 'slight', '常用款'),
('小提琴松香', 'rosin', '松香', '无尘轻型 4/4', 'violin', '小提琴', 3, '养护耗材', 'Pirastro', 180, '2026-05-01', 'good', ''),
('电吉他拨片', 'pick', '拨片', '0.88mm 尼龙防滑', 'guitar-electric', '电吉他', 1, '弹奏配件', 'Dunlop Tortex', 60, '2026-05-10', 'good', '5片装'),
('小提琴琴弓', 'bow', '琴弓', '4/4 巴西木 八角弓', 'violin', '小提琴', 1, '弹奏配件', '', 365, '2026-01-15', 'slight', ''),
('吉他变调夹', 'capo', '变调夹', '弹簧式 金属款', 'guitar-acoustic', '木吉他', 2, '辅助工具', 'Shubb C1', 730, '2025-11-20', 'good', ''),
('指板清洁剂', 'cleaner', '清洁用品', '柠檬油 100ml', 'guitar-acoustic', '木吉他', 3, '养护耗材', 'MusicNomad', 180, '2025-08-01', 'severe', '快用完了');

INSERT INTO replacement_record (accessory_id, accessory_name, specification, instrument_name, replace_date, standard_cycle, usage_days, operator, remark) VALUES
(1, '木吉他琴弦', '012-053 磷铜覆膜', '木吉他', '2026-01-20', 90, 85, '本人', '使用近三月'),
(1, '木吉他琴弦', '012-053 磷铜覆膜', '木吉他', '2026-04-15', 90, 60, '本人', '音色变闷，及时更换'),
(2, '小提琴松香', '无尘轻型 4/4', '小提琴', '2026-05-10', 180, 35, '本人', ''),
(3, '电吉他拨片', '0.88mm 尼龙防滑', '电吉他', '2026-05-25', 60, 20, '本人', '丢了一个，换新的'),
(6, '指板清洁剂', '柠檬油 100ml', '木吉他', '2026-03-01', 180, 105, '本人', '深度保养使用');
