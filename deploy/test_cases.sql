USE blog_cloud_test;

INSERT INTO tb_user(username, password, nickname, email, phone, status, role_id)
SELECT 'case_user_1',
       '$2a$10$q4r4W3oZz3z5V7rP6D5N2O5b3nf2zvM3Y1ZQf3aD9sV2Q4w8nQd8S',
       'CaseUser1',
       'case1@example.com',
       '13800000001',
       1,
       2
WHERE NOT EXISTS (SELECT 1 FROM tb_user WHERE username = 'case_user_1');

INSERT INTO tb_article(title, summary, content, author_id, board_id, tags, status, view_count, comment_count, like_count, favorite_count, is_top, is_essence, allow_comment)
SELECT 'CASE_ARTICLE_1', 'case summary', 'case content', u.id, 1, 'case', 1, 0, 0, 0, 0, 0, 0, 1
FROM tb_user u
WHERE u.username = 'case_user_1'
  AND NOT EXISTS (SELECT 1 FROM tb_article WHERE title = 'CASE_ARTICLE_1');

INSERT INTO tb_notify(user_id, type, title, content, article_id, comment_id, sender_id, is_read)
SELECT u.id, 1, 'CASE_NOTIFY_1', 'case notify content', a.id, NULL, u.id, 0
FROM tb_user u
JOIN tb_article a ON a.title = 'CASE_ARTICLE_1'
WHERE u.username = 'case_user_1'
  AND NOT EXISTS (SELECT 1 FROM tb_notify WHERE title = 'CASE_NOTIFY_1');
