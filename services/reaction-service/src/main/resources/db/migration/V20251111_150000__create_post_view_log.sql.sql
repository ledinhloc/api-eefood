-- Tạo bảng post_view_log
CREATE TABLE post_view_log (
   id BIGSERIAL PRIMARY KEY,
   user_id BIGINT NOT NULL,
   post_id BIGINT NOT NULL,
   viewed_at TIMESTAMP NOT NULL,
   view_duration BIGINT NOT NULL,

   CONSTRAINT fk_post FOREIGN KEY (post_id) REFERENCES post(id)
);

-- Dữ liệu cho bảng post_view_log
INSERT INTO post_view_log (user_id, post_id, viewed_at, view_duration)
VALUES
    (1, 1,  NOW() - INTERVAL '11 minutes', 30),
    (1, 2,  NOW() - INTERVAL '10 minutes', 45),
    (1, 3,  NOW() - INTERVAL '9 minutes', 20),
    (2, 4,  NOW() - INTERVAL '8 minutes', 25),
    (2, 5,  NOW() - INTERVAL '7 minutes', 15),
    (2, 6,  NOW() - INTERVAL '6 minutes', 50),
    (3, 7,  NOW() - INTERVAL '5 minutes', 40),
    (3, 8,  NOW() - INTERVAL '4 minutes', 35),
    (4, 9,  NOW() - INTERVAL '3 minutes', 55),
    (4, 10, NOW() - INTERVAL '2 minutes', 60),
    (5, 11, NOW() - INTERVAL '1 minute', 42);