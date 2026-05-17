SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_article_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_article_column_if_missing(IN p_column_name VARCHAR(64), IN p_column_definition TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'tb_article'
          AND column_name = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE tb_article ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_article_column_if_missing('hot_score', 'hot_score DOUBLE NOT NULL DEFAULT 0 AFTER allow_comment');
CALL add_article_column_if_missing('hot_adjust_score', 'hot_adjust_score DOUBLE NOT NULL DEFAULT 0 AFTER hot_score');
CALL add_article_column_if_missing('hot_decay_enabled', 'hot_decay_enabled TINYINT NOT NULL DEFAULT 1 AFTER hot_adjust_score');
CALL add_article_column_if_missing('last_hot_refresh_time', 'last_hot_refresh_time DATETIME DEFAULT CURRENT_TIMESTAMP AFTER hot_decay_enabled');
DROP PROCEDURE IF EXISTS add_article_column_if_missing;

DROP PROCEDURE IF EXISTS add_article_hot_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_article_hot_index_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'tb_article'
          AND index_name = 'idx_article_status_hot_score'
    ) THEN
        CREATE INDEX idx_article_status_hot_score ON tb_article(status, hot_score, id);
    END IF;
END//
DELIMITER ;
CALL add_article_hot_index_if_missing();
DROP PROCEDURE IF EXISTS add_article_hot_index_if_missing;
