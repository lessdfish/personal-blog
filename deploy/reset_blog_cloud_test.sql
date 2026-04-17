USE blog_cloud_test;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE tb_notify;
TRUNCATE TABLE tb_comment;
TRUNCATE TABLE tb_article_like;
TRUNCATE TABLE tb_article_favorite;
TRUNCATE TABLE tb_article;
TRUNCATE TABLE tb_user;
TRUNCATE TABLE tb_role_permission;
TRUNCATE TABLE tb_permission;
TRUNCATE TABLE tb_board;
TRUNCATE TABLE tb_role;
TRUNCATE TABLE perf_numbers;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO tb_role (id, role_name, role_code, description)
VALUES
    (1, '超级管理员', 'ADMIN', '全站管理'),
    (2, '普通用户', 'USER', '普通发帖用户'),
    (3, '版主', 'MODERATOR', '版块运营与内容管理');

INSERT INTO tb_permission (id, permission_code, permission_name, description)
VALUES
    (1, 'user:manage', '用户管理', '查看与管理用户列表'),
    (2, 'role:view', '角色查看', '查看角色与权限配置'),
    (3, 'role:assign', '角色分配', '为用户分配角色'),
    (4, 'stats:view', '统计查看', '查看活跃用户与站点统计');

INSERT INTO tb_role_permission (role_id, permission_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4),
    (3, 4);

INSERT INTO tb_board (id, board_name, board_code, description, sort_order, status)
VALUES
    (1, '综合讨论', 'general', '默认综合交流版块', 1, 1),
    (2, '技术交流', 'tech', '技术、开发与踩坑分享', 2, 1),
    (3, '资源分享', 'share', '教程、资料与工具分享', 3, 1);

INSERT INTO tb_user (id, username, password, nickname, avatar, email, phone, status, role_id)
VALUES
    (1, 'admin', '$2a$10$IFRYa.cupDVCAJ7QdwYs4.wXsYAyjCfFm79W4./FG0tynK7A53aoe', 'admin', NULL, NULL, NULL, 1, 1);
