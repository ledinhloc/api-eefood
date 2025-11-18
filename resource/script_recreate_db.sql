-- Ngắt kết nối tất cả session đang dùng DB iam_service (nếu có)
-- vì PostgreSQL không cho DROP DATABASE khi đang có kết nối active
-- recipe_service, notification_service, reaction_service
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'recipe_service'
  AND pid <> pg_backend_pid();

-- Xóa database nếu tồn tại
DROP DATABASE IF EXISTS recipe_service;

-- Tạo database mới với encoding UTF8
CREATE DATABASE recipe_service
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    CONNECTION LIMIT = -1;
