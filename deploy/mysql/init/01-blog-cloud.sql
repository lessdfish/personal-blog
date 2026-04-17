CREATE DATABASE IF NOT EXISTS blog_cloud CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog_cloud;

CREATE TABLE IF NOT EXISTS tb_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS tb_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL UNIQUE,
    avatar VARCHAR(255) NULL,
    email VARCHAR(100) NULL UNIQUE,
    phone VARCHAR(20) NULL UNIQUE,
    status TINYINT NOT NULL DEFAULT 1,
    role_id BIGINT NOT NULL DEFAULT 2,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_role_id (role_id)
);

CREATE TABLE IF NOT EXISTS tb_board (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    board_name VARCHAR(100) NOT NULL,
    board_code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    sort_order INT DEFAULT 99,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_board_status_sort (status, sort_order)
);

CREATE TABLE IF NOT EXISTS tb_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NULL,
    content LONGTEXT NOT NULL,
    author_id BIGINT NOT NULL,
    board_id BIGINT NULL,
    tags VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    view_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    is_top TINYINT NOT NULL DEFAULT 0,
    is_essence TINYINT NOT NULL DEFAULT 0,
    allow_comment TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_article_status_top_board_id (status, is_top, board_id, id),
    KEY idx_article_author_status_id (author_id, status, id)
);

CREATE TABLE IF NOT EXISTS tb_article_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_like_user (article_id, user_id),
    KEY idx_article_like_user (user_id, article_id)
);

CREATE TABLE IF NOT EXISTS tb_article_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_favorite_user (article_id, user_id),
    KEY idx_favorite_user_article (user_id, article_id)
);

CREATE TABLE IF NOT EXISTS tb_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    notify_user_id BIGINT NULL,
    content TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_comment_article_status_parent_id (article_id, status, parent_id, id)
);

CREATE TABLE IF NOT EXISTS tb_notify (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type TINYINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    article_id BIGINT NULL,
    comment_id BIGINT NULL,
    sender_id BIGINT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notify_user_read_time (user_id, is_read, create_time),
    KEY idx_notify_user_page_cover (user_id, create_time, id)
);

INSERT INTO tb_role (id, role_name, role_code, description)
VALUES
    (1, '超级管理员', 'ADMIN', '全站管理'),
    (2, '普通用户', 'USER', '普通发帖用户'),
    (3, '版主', 'MODERATOR', '版块运营与内容管理')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description);

INSERT INTO tb_permission (id, permission_code, permission_name, description)
VALUES
    (1, 'user:manage', '用户管理', '查看与管理用户列表'),
    (2, 'role:view', '角色查看', '查看角色与权限配置'),
    (3, 'role:assign', '角色分配', '为用户分配角色'),
    (4, 'stats:view', '统计查看', '查看活跃用户与站点统计')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    description = VALUES(description);

INSERT INTO tb_role_permission (role_id, permission_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4),
    (3, 4)
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id);

INSERT INTO tb_board (id, board_name, board_code, description, sort_order, status)
VALUES
    (1, '综合讨论', 'general', '默认综合交流版块', 1, 1),
    (2, '技术交流', 'tech', '技术、开发与踩坑分享', 2, 1),
    (3, '资源分享', 'share', '教程、资料与工具分享', 3, 1)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO tb_user (id, username, password, nickname, avatar, email, phone, status, role_id)
VALUES
    (1, 'admin', '$2a$10$IFRYa.cupDVCAJ7QdwYs4.wXsYAyjCfFm79W4./FG0tynK7A53aoe', 'admin', NULL, NULL, NULL, 1, 1)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    status = VALUES(status),
    role_id = VALUES(role_id);

