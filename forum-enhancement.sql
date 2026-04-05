ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS role_id BIGINT DEFAULT 2;

ALTER TABLE tb_article
    ADD COLUMN IF NOT EXISTS summary VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS board_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS tags VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS comment_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS like_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS favorite_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_top TINYINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_essence TINYINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS allow_comment TINYINT DEFAULT 1;

CREATE TABLE IF NOT EXISTS tb_board (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    board_name VARCHAR(100) NOT NULL,
    board_code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    sort_order INT DEFAULT 99,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_article_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id)
);

CREATE INDEX idx_article_status_top_board_id ON tb_article(status, is_top, board_id, id);
CREATE INDEX idx_article_author_status_id ON tb_article(author_id, status, id);
CREATE INDEX idx_comment_article_status_parent_id ON tb_comment(article_id, status, parent_id, id);
CREATE INDEX idx_notify_user_read_time ON tb_notify(user_id, is_read, create_time);
CREATE INDEX idx_favorite_user_article ON tb_article_favorite(user_id, article_id);
CREATE INDEX idx_role_permission_permission ON tb_role_permission(permission_id);

CREATE TABLE IF NOT EXISTS tb_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS tb_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

INSERT INTO tb_board(board_name, board_code, description, sort_order, status)
VALUES
    ('综合讨论', 'general', '默认综合交流版块', 1, 1),
    ('技术交流', 'tech', '技术、开发与踩坑分享', 2, 1),
    ('资源分享', 'share', '教程、资料与工具分享', 3, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO tb_permission(permission_code, permission_name, description)
VALUES
    ('user:manage', '用户管理', '查看与管理用户列表'),
    ('role:view', '角色查看', '查看角色与权限配置'),
    ('role:assign', '角色分配', '为用户分配角色'),
    ('stats:view', '统计查看', '查看活跃用户与站点统计')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), description = VALUES(description);

INSERT INTO tb_role(role_name, role_code, description)
VALUES
    ('超级管理员', 'ADMIN', '全站管理'),
    ('普通用户', 'USER', '普通发帖用户'),
    ('版主', 'MODERATOR', '版块运营与内容管理')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

INSERT INTO tb_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO tb_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p
WHERE r.role_code = 'MODERATOR'
  AND p.permission_code IN ('stats:view')
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
