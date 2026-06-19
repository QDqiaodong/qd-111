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

DROP TABLE IF EXISTS preparation_template;
CREATE TABLE preparation_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL COMMENT '配件类型编码',
    type_name VARCHAR(50) DEFAULT NULL COMMENT '配件类型名称',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    item_count INT DEFAULT 0 COMMENT '准备项数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_type_code (type_code),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更换前准备清单模板';

DROP TABLE IF EXISTS preparation_template_item;
CREATE TABLE preparation_template_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    category VARCHAR(50) NOT NULL COMMENT '准备项分类：change-换弦/更换, clean-擦拭清洁, adjust-调试校准, check-检查确认, tool-工具准备, other-其他',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    name VARCHAR(100) NOT NULL COMMENT '准备项名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '准备项说明',
    required TINYINT DEFAULT 1 COMMENT '是否必做 0-选做 1-必做',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_template_id (template_id),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='准备清单模板明细';

DROP TABLE IF EXISTS preparation_checklist;
CREATE TABLE preparation_checklist (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '关联模板ID',
    template_name VARCHAR(100) DEFAULT NULL COMMENT '模板名称快照',
    type_code VARCHAR(50) NOT NULL COMMENT '配件类型编码',
    type_name VARCHAR(50) DEFAULT NULL COMMENT '配件类型名称',
    accessory_id BIGINT DEFAULT NULL COMMENT '配件ID',
    accessory_name VARCHAR(100) DEFAULT NULL COMMENT '配件名称快照',
    replacement_record_id BIGINT DEFAULT NULL COMMENT '关联更换记录ID',
    operator VARCHAR(50) DEFAULT NULL COMMENT '操作人',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待开始, in_progress-进行中, completed-已完成',
    total_count INT DEFAULT 0 COMMENT '总项数',
    completed_count INT DEFAULT 0 COMMENT '已完成项数',
    required_completed_count INT DEFAULT 0 COMMENT '必做项已完成数',
    required_total_count INT DEFAULT 0 COMMENT '必做项总数',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_template_id (template_id),
    KEY idx_accessory_id (accessory_id),
    KEY idx_replacement_record_id (replacement_record_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更换前准备清单执行记录';

DROP TABLE IF EXISTS preparation_checklist_item;
CREATE TABLE preparation_checklist_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    checklist_id BIGINT NOT NULL COMMENT '清单执行记录ID',
    template_item_id BIGINT NOT NULL COMMENT '模板项ID',
    category VARCHAR(50) NOT NULL COMMENT '准备项分类',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    name VARCHAR(100) NOT NULL COMMENT '准备项名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '准备项说明',
    required TINYINT DEFAULT 1 COMMENT '是否必做',
    sort_order INT DEFAULT 0 COMMENT '排序',
    completed TINYINT DEFAULT 0 COMMENT '是否完成 0-未完成 1-已完成',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    completed_by VARCHAR(50) DEFAULT NULL COMMENT '完成人',
    completion_note VARCHAR(500) DEFAULT NULL COMMENT '完成备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_checklist_id (checklist_id),
    KEY idx_template_item_id (template_item_id),
    KEY idx_completed (completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='准备清单执行明细';

INSERT INTO preparation_template (type_code, type_name, name, description, enabled, item_count) VALUES
('string', '琴弦', '琴弦更换准备清单', '更换琴弦前的标准准备工作流程', 1, 12),
('bow', '琴弓', '琴弓更换准备清单', '更换琴弓前的标准准备工作流程', 1, 8),
('pick', '拨片', '拨片更换准备清单', '更换拨片前的准备工作', 1, 5),
('rosin', '松香', '松香更换准备清单', '更换松香前的准备工作', 1, 5),
('cleaner', '清洁用品', '清洁用品使用准备清单', '使用清洁用品前的准备工作', 1, 6),
('capo', '变调夹', '变调夹更换准备清单', '更换变调夹前的准备工作', 1, 4),
('strap', '背带', '背带更换准备清单', '更换背带前的准备工作', 1, 4);

DROP TABLE IF EXISTS worn_status_dict;
CREATE TABLE worn_status_dict (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    status_code VARCHAR(20) NOT NULL COMMENT '状态编码',
    status_label VARCHAR(50) NOT NULL COMMENT '状态名称',
    color VARCHAR(20) DEFAULT '#909399' COMMENT '状态颜色',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_status_code (status_code),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='损耗状态字典';

INSERT INTO worn_status_dict (status_code, status_label, color, sort_order, remark) VALUES
('good', '完好', '#67c23a', 1, '配件状态良好，可正常使用'),
('slight', '轻微磨损', '#e6a23c', 2, '有轻微磨损痕迹，不影响使用'),
('severe', '严重损耗', '#f56c6c', 3, '损耗较严重，建议更换'),
('broken', '已损坏', '#909399', 4, '已损坏，无法使用');

INSERT INTO preparation_template_item (template_id, category, category_name, name, description, required, sort_order) VALUES
(1, 'tool', '工具准备', '准备新琴弦', '确认新琴弦规格、品牌型号正确，检查包装完好', 1, 1),
(1, 'tool', '工具准备', '准备换弦工具', '准备卷弦器、剪弦钳、调音器等必要工具', 1, 2),
(1, 'tool', '工具准备', '准备清洁用品', '准备指板清洁剂、擦布、护理油等', 0, 3),
(1, 'clean', '擦拭清洁', '卸下旧琴弦', '小心松开并卸下旧琴弦，注意避免琴码弹跳', 1, 4),
(1, 'clean', '擦拭清洁', '清洁指板', '使用清洁剂和擦布彻底清洁指板，去除污垢和汗渍', 1, 5),
(1, 'clean', '擦拭清洁', '清洁琴身', '擦拭琴身表面，清理灰尘和指纹', 0, 6),
(1, 'clean', '擦拭清洁', '清洁琴桥和琴码', '清理琴桥和琴码处的积灰和旧弦残留', 0, 7),
(1, 'check', '检查确认', '检查指板状态', '检查指板是否有干裂、变形，品丝是否磨损', 1, 8),
(1, 'check', '检查确认', '检查琴桥状态', '检查琴桥是否稳固，下弦枕是否磨损', 1, 9),
(1, 'check', '检查确认', '检查琴颈状态', '检查琴颈弯曲度，确认是否需要调节', 0, 10),
(1, 'adjust', '调试校准', '安装新琴弦', '按照正确顺序安装新琴弦，注意留足余量', 1, 11),
(1, 'adjust', '调试校准', '初步调音', '将新弦调至标准音高，预留拉伸余量', 1, 12),
(2, 'tool', '工具准备', '准备新琴弓', '确认新琴弓尺寸、材质、重量符合要求', 1, 1),
(2, 'tool', '工具准备', '准备松香', '准备适用的松香', 1, 2),
(2, 'clean', '擦拭清洁', '清洁琴身', '擦拭琴身，清理灰尘', 1, 3),
(2, 'clean', '擦拭清洁', '清洁指板', '清洁指板，去除汗渍和污垢', 0, 4),
(2, 'check', '检查确认', '检查琴码位置', '确认琴码位置正确，无倾斜', 1, 5),
(2, 'check', '检查确认', '检查琴桥', '检查琴桥状态，确认无开裂', 1, 6),
(2, 'adjust', '调试校准', '检查琴弓马尾', '检查新琴弓马尾是否整齐，长度合适', 1, 7),
(2, 'adjust', '调试校准', '上松香', '均匀涂抹松香，测试发音', 1, 8),
(3, 'tool', '工具准备', '准备新拨片', '确认新拨片厚度、材质、形状符合需求', 1, 1),
(3, 'clean', '擦拭清洁', '擦拭琴弦', '擦拭琴弦，去除油渍和汗渍', 0, 2),
(3, 'check', '检查确认', '检查琴弦状态', '检查琴弦是否有锈迹、断丝', 1, 3),
(3, 'check', '检查确认', '检查品丝', '检查品丝是否有磨损凹陷', 0, 4),
(3, 'adjust', '调试校准', '测试音色', '用新拨片测试各弦音色', 1, 5),
(4, 'tool', '工具准备', '准备新松香', '确认新松香型号适配乐器', 1, 1),
(4, 'clean', '擦拭清洁', '清洁琴弓马尾', '清理马尾上的旧松香残留', 1, 2),
(4, 'clean', '擦拭清洁', '擦拭琴身', '擦拭琴身表面', 0, 3),
(4, 'check', '检查确认', '检查琴弓马尾状态', '检查马尾是否有断裂、松脱', 1, 4),
(4, 'adjust', '调试校准', '涂抹新松香', '均匀涂抹松香，测试发音', 1, 5),
(5, 'tool', '工具准备', '准备清洁用品', '确认清洁剂、护理油、擦布等齐全', 1, 1),
(5, 'tool', '工具准备', '准备备用配件', '如有需要，准备更换的配件', 0, 2),
(5, 'clean', '擦拭清洁', '表面除尘', '先用干布去除表面浮尘', 1, 3),
(5, 'clean', '擦拭清洁', '深层清洁', '使用清洁剂进行深层清洁', 1, 4),
(5, 'check', '检查确认', '检查清洁效果', '确认无残留、无损伤', 1, 5),
(5, 'adjust', '调试校准', '护理保养', '涂抹护理油，进行保养', 1, 6),
(6, 'tool', '工具准备', '准备新变调夹', '确认变调夹类型、材质符合需求', 1, 1),
(6, 'check', '检查确认', '检查指板', '检查指板和品丝状态', 1, 2),
(6, 'check', '检查确认', '检查琴弦', '检查琴弦状态是否正常', 0, 3),
(6, 'adjust', '调试校准', '测试变调夹', '测试变调夹夹持力度和音准', 1, 4),
(7, 'tool', '工具准备', '准备新背带', '确认背带宽度、材质、长度合适', 1, 1),
(7, 'check', '检查确认', '检查背带扣', '检查琴身背带扣是否牢固', 1, 2),
(7, 'check', '检查确认', '检查琴身', '检查琴身连接部位', 0, 3),
(7, 'adjust', '调试校准', '调节背带长度', '调节到合适长度，测试舒适度', 1, 4);
