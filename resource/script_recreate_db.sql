-- Định nghĩa biến cho tên database
\set db_name 'iam_service'

-- Ngắt kết nối tất cả session đang dùng DB (nếu có)
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = :'db_name'
  AND pid <> pg_backend_pid();

-- Xóa database nếu tồn tại
DROP DATABASE IF EXISTS :db_name;

-- Tạo database mới với encoding UTF8
CREATE DATABASE :db_name
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    CONNECTION LIMIT = -1;