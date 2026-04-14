USE blog_cloud_test;

DELETE FROM tb_notify
WHERE title LIKE 'PERF_%'
   OR article_id IN (SELECT id FROM (SELECT id FROM tb_article WHERE title LIKE 'PERF_%') tmp_article)
   OR sender_id IN (SELECT id FROM (SELECT id FROM tb_user WHERE username LIKE 'perfuser%') tmp_user1)
   OR user_id IN (SELECT id FROM (SELECT id FROM tb_user WHERE username LIKE 'perfuser%') tmp_user2);

DELETE FROM tb_comment
WHERE user_id IN (SELECT id FROM (SELECT id FROM tb_user WHERE username LIKE 'perfuser%') tmp_user)
   OR article_id IN (SELECT id FROM (SELECT id FROM tb_article WHERE title LIKE 'PERF_%') tmp_article);

DELETE FROM tb_article_like
WHERE user_id IN (SELECT id FROM (SELECT id FROM tb_user WHERE username LIKE 'perfuser%') tmp_user1)
   OR article_id IN (SELECT id FROM (SELECT id FROM tb_article WHERE title LIKE 'PERF_%') tmp_article1);

DELETE FROM tb_article_favorite
WHERE user_id IN (SELECT id FROM (SELECT id FROM tb_user WHERE username LIKE 'perfuser%') tmp_user2)
   OR article_id IN (SELECT id FROM (SELECT id FROM tb_article WHERE title LIKE 'PERF_%') tmp_article2);

DELETE FROM tb_article WHERE title LIKE 'PERF_%';
DELETE FROM tb_user WHERE username LIKE 'perfuser%';

UPDATE tb_article a
LEFT JOIN (
    SELECT article_id, COUNT(*) AS comment_cnt
    FROM tb_comment
    GROUP BY article_id
) c ON a.id = c.article_id
LEFT JOIN (
    SELECT article_id, COUNT(*) AS like_cnt
    FROM tb_article_like
    GROUP BY article_id
) l ON a.id = l.article_id
LEFT JOIN (
    SELECT article_id, COUNT(*) AS favorite_cnt
    FROM tb_article_favorite
    GROUP BY article_id
) f ON a.id = f.article_id
SET a.comment_count = IFNULL(c.comment_cnt, 0),
    a.like_count = IFNULL(l.like_cnt, 0),
    a.favorite_count = IFNULL(f.favorite_cnt, 0);
