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
    image_width INT DEFAULT NULL COMMENT '图片宽度(px)',
    image_height INT DEFAULT NULL COMMENT '图片高度(px)',
    image_size BIGINT DEFAULT NULL COMMENT '图片大小(byte)',
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

DROP TABLE IF EXISTS standard_cycle_rule;
CREATE TABLE standard_cycle_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL COMMENT '配件类型编码',
    type_name VARCHAR(50) DEFAULT NULL COMMENT '配件类型名称',
    instrument VARCHAR(50) DEFAULT NULL COMMENT '适配乐器编码，NULL表示通用',
    instrument_name VARCHAR(50) DEFAULT NULL COMMENT '适配乐器名称',
    spec_pattern VARCHAR(200) DEFAULT NULL COMMENT '规格匹配模式（支持模糊匹配，NULL表示通用）',
    spec_description VARCHAR(500) DEFAULT NULL COMMENT '规格描述',
    standard_cycle INT NOT NULL COMMENT '标准更换周期(天)',
    priority INT DEFAULT 0 COMMENT '匹配优先级，数值越大优先级越高',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_type_code (type_code),
    KEY idx_instrument (instrument),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准更换周期规则库';

INSERT INTO standard_cycle_rule (type_code, type_name, instrument, instrument_name, spec_pattern, spec_description, standard_cycle, priority, remark) VALUES
('string', '琴弦', 'guitar-acoustic', '木吉他', '010', '轻张力琴弦（010系列）', 120, 10, '轻张力琴弦磨损较慢'),
('string', '琴弦', 'guitar-acoustic', '木吉他', '011', '中等张力琴弦（011系列）', 100, 10, '中等张力琴弦'),
('string', '琴弦', 'guitar-acoustic', '木吉他', '012', '标准张力琴弦（012系列）', 90, 10, '标准张力琴弦，常用款'),
('string', '琴弦', 'guitar-acoustic', '木吉他', '013', '重张力琴弦（013系列）', 80, 10, '重张力琴弦磨损较快'),
('string', '琴弦', 'guitar-electric', '电吉他', '009', '电吉他轻张力（009系列）', 90, 10, '电吉他常用轻张力'),
('string', '琴弦', 'guitar-electric', '电吉他', '010', '电吉他标准张力（010系列）', 75, 10, '电吉他标准款'),
('string', '琴弦', 'guitar-electric', '电吉他', '011', '电吉他重张力（011系列）', 60, 10, '电吉他重张力，适合降弦'),
('string', '琴弦', 'guitar-bass', '贝斯', '040', '贝斯轻张力（040系列）', 180, 10, '贝斯琴弦磨损较慢'),
('string', '琴弦', 'guitar-bass', '贝斯', '045', '贝斯标准张力（045系列）', 150, 10, '贝斯标准款'),
('string', '琴弦', 'guitar-bass', '贝斯', '050', '贝斯重张力（050系列）', 120, 10, '贝斯重张力'),
('string', '琴弦', 'violin', '小提琴', '4/4', '小提琴4/4尺寸', 90, 10, '小提琴标准款'),
('string', '琴弦', 'violin', '小提琴', '3/4', '小提琴3/4尺寸', 90, 10, '小提琴3/4尺寸'),
('string', '琴弦', 'violin', '小提琴', '1/2', '小提琴1/2尺寸', 90, 10, '小提琴1/2尺寸'),
('string', '琴弦', 'ukulele', '尤克里里', NULL, '尤克里里琴弦通用', 180, 5, '尤克里里琴弦通用周期'),
('string', '琴弦', 'erhu', '二胡', NULL, '二胡琴弦通用', 180, 5, '二胡琴弦通用周期'),
('string', '琴弦', NULL, '通用', NULL, '琴弦通用周期', 90, 0, '琴弦通用默认周期'),
('bow', '琴弓', 'violin', '小提琴', '4/4', '小提琴4/4琴弓', 365, 10, '小提琴琴弓'),
('bow', '琴弓', 'violin', '小提琴', '3/4', '小提琴3/4琴弓', 365, 10, '小提琴3/4琴弓'),
('bow', '琴弓', 'violin', '小提琴', '1/2', '小提琴1/2琴弓', 365, 10, '小提琴1/2琴弓'),
('bow', '琴弓', 'erhu', '二胡', NULL, '二胡琴弓', 365, 10, '二胡琴弓'),
('bow', '琴弓', NULL, '通用', NULL, '琴弓通用周期', 365, 0, '琴弓通用默认周期'),
('pick', '拨片', 'guitar-acoustic', '木吉他', '0.60', '薄拨片（0.60mm以下）', 90, 10, '薄拨片适合扫弦，磨损较慢'),
('pick', '拨片', 'guitar-acoustic', '木吉他', '0.73', '中薄拨片（0.73mm）', 75, 10, '中薄拨片'),
('pick', '拨片', 'guitar-acoustic', '木吉他', '0.88', '中等厚度拨片（0.88mm）', 60, 10, '中等厚度，通用款'),
('pick', '拨片', 'guitar-acoustic', '木吉他', '1.0', '厚拨片（1.0mm以上）', 50, 10, '厚拨片适合独奏'),
('pick', '拨片', 'guitar-electric', '电吉他', '0.60', '薄拨片（0.60mm以下）', 90, 10, '电吉他薄拨片'),
('pick', '拨片', 'guitar-electric', '电吉他', '0.73', '中薄拨片（0.73mm）', 75, 10, '电吉他中薄拨片'),
('pick', '拨片', 'guitar-electric', '电吉他', '0.88', '中等厚度拨片（0.88mm）', 60, 10, '电吉他中等厚度'),
('pick', '拨片', 'guitar-electric', '电吉他', '1.0', '厚拨片（1.0mm以上）', 45, 10, '电吉他厚拨片，速弹磨损快'),
('pick', '拨片', 'guitar-bass', '贝斯', '1.0', '贝斯薄拨片（1.0mm以下）', 90, 10, '贝斯薄拨片'),
('pick', '拨片', 'guitar-bass', '贝斯', '1.5', '贝斯厚拨片（1.5mm以上）', 75, 10, '贝斯厚拨片'),
('pick', '拨片', 'ukulele', '尤克里里', NULL, '尤克里里拨片', 120, 10, '尤克里里拨片'),
('pick', '拨片', NULL, '通用', NULL, '拨片通用周期', 60, 0, '拨片通用默认周期'),
('rosin', '松香', 'violin', '小提琴', NULL, '小提琴松香', 180, 10, '小提琴松香'),
('rosin', '松香', 'erhu', '二胡', NULL, '二胡松香', 180, 10, '二胡松香'),
('rosin', '松香', NULL, '通用', NULL, '松香通用周期', 180, 0, '松香通用默认周期'),
('capo', '变调夹', 'guitar-acoustic', '木吉他', NULL, '木吉他变调夹', 730, 10, '木吉他变调夹'),
('capo', '变调夹', 'guitar-electric', '电吉他', NULL, '电吉他变调夹', 730, 10, '电吉他变调夹'),
('capo', '变调夹', 'ukulele', '尤克里里', NULL, '尤克里里变调夹', 730, 10, '尤克里里变调夹'),
('capo', '变调夹', NULL, '通用', NULL, '变调夹通用周期', 730, 0, '变调夹通用默认周期'),
('strap', '背带', NULL, '通用', NULL, '背带通用周期', 730, 0, '背带通用默认周期'),
('cleaner', '清洁用品', NULL, '通用', NULL, '清洁用品通用周期', 180, 0, '清洁用品通用默认周期'),
('other', '其他', NULL, '通用', NULL, '其他配件通用周期', 365, 0, '其他配件通用默认周期');

INSERT INTO accessory (name, type_code, type_name, specification, instrument, instrument_name, group_id, group_name, brand_model, standard_cycle, purchase_date, worn_status, remark) VALUES
('木吉他拨片', 'pick', '拨片', '0.73mm 尼龙', 'guitar-acoustic', '木吉他', 1, '弹奏配件', 'Dunlop Tortex', 75, '2026-05-18', 'good', '中薄拨片'),
('吉他背带', 'strap', '背带', '棉质 5cm 宽', 'guitar-acoustic', '木吉他', 2, '辅助工具', 'Ernie Ball', 730, '2025-12-01', 'good', '');

DROP TABLE IF EXISTS accessory_set;
CREATE TABLE accessory_set (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '套装名称',
    instrument VARCHAR(50) NOT NULL COMMENT '适配乐器编码',
    instrument_name VARCHAR(50) DEFAULT NULL COMMENT '适配乐器名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '套装说明',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    status VARCHAR(20) DEFAULT 'enabled' COMMENT '状态 enabled-启用 disabled-停用',
    item_count INT DEFAULT 0 COMMENT '配件项数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_instrument (instrument),
    KEY idx_name (name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套装耗材档案';

DROP TABLE IF EXISTS accessory_set_item;
CREATE TABLE accessory_set_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    set_id BIGINT NOT NULL COMMENT '套装ID',
    accessory_id BIGINT NOT NULL COMMENT '配件ID',
    accessory_name VARCHAR(100) DEFAULT NULL COMMENT '配件名称快照',
    type_code VARCHAR(50) DEFAULT NULL COMMENT '配件类型编码',
    type_name VARCHAR(50) DEFAULT NULL COMMENT '配件类型名称',
    specification VARCHAR(500) DEFAULT NULL COMMENT '规格快照',
    instrument VARCHAR(50) DEFAULT NULL COMMENT '适配乐器编码',
    instrument_name VARCHAR(50) DEFAULT NULL COMMENT '适配乐器名称',
    group_id BIGINT DEFAULT NULL COMMENT '所属分组ID',
    group_name VARCHAR(50) DEFAULT NULL COMMENT '所属分组名称',
    quantity INT DEFAULT 1 COMMENT '数量',
    sort_order INT DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_set_id (set_id),
    KEY idx_accessory_id (accessory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套装耗材明细';

INSERT INTO accessory_set (name, instrument, instrument_name, description, status, item_count) VALUES
('木吉他日常保养套装', 'guitar-acoustic', '木吉他', '木吉他日常弹奏与保养常用耗材组合', 'enabled', 5);

INSERT INTO accessory_set_item (set_id, accessory_id, accessory_name, type_code, type_name, specification, instrument, instrument_name, group_id, group_name, quantity, sort_order, remark) VALUES
(1, 1, '木吉他琴弦', 'string', '琴弦', '012-053 磷铜覆膜', 'guitar-acoustic', '木吉他', 1, '弹奏配件', 1, 1, '核心耗材'),
(1, 7, '木吉他拨片', 'pick', '拨片', '0.73mm 尼龙', 'guitar-acoustic', '木吉他', 1, '弹奏配件', 5, 2, '消耗较快'),
(1, 8, '吉他背带', 'strap', '背带', '棉质 5cm 宽', 'guitar-acoustic', '木吉他', 2, '辅助工具', 1, 3, ''),
(1, 5, '吉他变调夹', 'capo', '变调夹', '弹簧式 金属款', 'guitar-acoustic', '木吉他', 2, '辅助工具', 1, 4, ''),
(1, 6, '指板清洁剂', 'cleaner', '清洁用品', '柠檬油 100ml', 'guitar-acoustic', '木吉他', 3, '养护耗材', 1, 5, '养护必备');
