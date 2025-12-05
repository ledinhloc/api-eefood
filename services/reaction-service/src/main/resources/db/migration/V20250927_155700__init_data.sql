-- =============================
-- Bảng post
-- =============================
INSERT INTO post (created_at, updated_at, created_by, updated_by, is_deleted, user_id, recipe_id, title, content, image_url)
VALUES
    (NOW(), NOW(), 'hoanganh', 'hoanganh', false, 1, 7, 'Bún bò Huế', 'Tô bún bò cay nồng chuẩn vị miền Trung', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq6UpPrSe9eYszz1gC0GUFxGepOZvX5hKkIw&s'),
    (NOW(), NOW(), 'ngocmai', 'ngocmai', false, 2, 5, 'Cơm tấm Sài Gòn', 'Sườn nướng, bì, chả trứng thơm ngon', 'https://file.hstatic.net/1000394081/article/com-tam_e03b4325c9914def9d66619930a73432.jpg'),
    (NOW(), NOW(), 'phuonglinh', 'phuonglinh', false, 3, 1, 'Phở bò Hà Nội', 'Nước dùng trong, thơm mùi quế hồi', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ5ffKuq9DTyRpBOg8ZGP01aszcSZcJ9fQXDw&s'),
    (NOW(), NOW(), 'quanghuy', 'quanghuy', false, 4, 2, 'Bánh mì trứng ốp la', 'Bữa sáng nhanh gọn nhưng đủ chất', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_BEyGNxXtr_7w8z-sVo0SushqWzwSvB6xiQ&s'),
    (NOW(), NOW(), 'lananh', 'lananh', false, 5, 3, 'Gỏi cuốn tôm thịt', 'Món ăn nhẹ, thanh mát, ít dầu mỡ', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT5kk3PGeI2UfstwOYq5tb15E1Bbsn53I6v_Q&s'),
    (NOW(), NOW(), 'minhtri', 'minhtri', false, 6, 8, 'Chả giò miền Nam', 'Giòn rụm, nhân thịt băm thơm phức', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTZt5emkGhgPt-K26Cq2oqT0HC0B4jtCm7xVQ&s'),
    (NOW(), NOW(), 'hoangphuc', 'hoangphuc', false, 7, 9, 'Bánh xèo tôm thịt', 'Vỏ vàng giòn, ăn cùng rau sống và nước mắm chua ngọt', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTQ2xosMlmOO05CRIspcYSrhvll2HTwnRYBYw&s'),
    (NOW(), NOW(), 'thanhha', 'thanhha', false, 8, 10, 'Cao lầu Hội An', 'Mì vàng dai, thịt xá xíu đậm vị', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQNsIEhWInFKJj4j9XcMgtvL9LNYCWv_kIkLQ&s'),
    (NOW(), NOW(), 'kimthao', 'kimthao', false, 9, 11, 'Bánh cuốn Thanh Trì', 'Bột gạo mỏng mịn, nhân thịt mộc nhĩ', 'https://static.vinwonders.com/production/banh-cuon-thanh-tri-banner.jpeg'),
    (NOW(), NOW(), 'tuananh', 'tuananh', false, 10, 12, 'Xôi xéo Hà Nội', 'Xôi nếp vàng ươm, thơm hành phi', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTLXHCgHafFLTjysi9B5c1qDkgbYs_ef_qGvw&s');

-- =============================
-- Bảng comment
-- =============================
INSERT INTO comment (created_at, updated_at, created_by, updated_by, is_deleted, user_id, parent_id, content, post_id)
VALUES
    (NOW(), NOW(), 'ngocmai', 'ngocmai', false, 2, NULL, 'Nhìn tô bún hấp dẫn quá!', 1),
    (NOW(), NOW(), 'quanghuy', 'quanghuy', false, 4, NULL, 'Phở bò này chắc ninh xương lâu lắm.', 3),
    (NOW(), NOW(), 'lananh', 'lananh', false, 5, 1, 'Chuẩn luôn, cay cay ăn đã miệng lắm!', 1),
    (NOW(), NOW(), 'hoangphuc', 'hoangphuc', false, 7, NULL, 'Bánh mì này thêm pate là đỉnh luôn.', 4),
    (NOW(), NOW(), 'thanhha', 'thanhha', false, 8, NULL, 'Cao lầu mà thêm tóp mỡ thì hết sẩy.', 8),
    (NOW(), NOW(), 'hoanganh', 'hoanganh', false, 1, NULL, 'Xôi xéo nhà mình ăn sáng suốt.', 10),
    (NOW(), NOW(), 'phuonglinh', 'phuonglinh', false, 3, NULL, 'Gỏi cuốn nhìn thanh mát thật!', 5),
    (NOW(), NOW(), 'tuananh', 'tuananh', false, 10, NULL, 'Bánh cuốn này ăn với nước mắm tỏi ớt là chuẩn.', 9),
    (NOW(), NOW(), 'minhtri', 'minhtri', false, 6, NULL, 'Chả giò vàng đều đẹp ghê.', 6),
    (NOW(), NOW(), 'kimthao', 'kimthao', false, 9, NULL, 'Cơm tấm lúc nào cũng ngon.', 2);
-- =============================
-- Bảng comment image
-- =============================
INSERT INTO comment_images (comment_id, image_url)
VALUES
    (1, 'https://png.pngtree.com/png-vector/20220608/ourlarge/pngtree-yummy-smile-emoji-with-tongue-lick-mouth-png-image_4915077.png'),
    (1, 'https://cdn2.fptshop.com.vn/unsafe/1920x0/filters:format(webp):quality(75)/2023_10_26_638339206554757767_bun-bo-01.jpeg'),
    (3, 'https://cdn2.fptshop.com.vn/unsafe/1920x0/filters:format(webp):quality(75)/2023_10_26_638339206554757767_bun-bo-01.jpeg');

-- =============================
--  Bảng comment_videos
-- =============================
INSERT INTO comment_videos (comment_id, video_url)
VALUES
    (2, 'https://res.cloudinary.com/dlkvdusla/video/upload/v1744332430/tewv0bgqobiugi2uhbir.mp4'),
    (5, 'https://res.cloudinary.com/dlkvdusla/video/upload/v1744332434/emsynvy2eexlokqdnj3c.mp4');
-- =============================
--  Bảng comment_reaction
-- =============================
INSERT INTO comment_reaction (comment_id, user_id, reaction_type, created_at)
VALUES
    (1, 3, 'LIKE', NOW()),
    (1, 5, 'LOVE', NOW()),
    (2, 1, 'LIKE', NOW()),
    (2, 4, 'WOW', NOW()),
    (3, 8, 'LIKE', NOW()),
    (4, 2, 'LOVE', NOW()),
    (5, 3, 'LIKE', NOW()),
    (6, 5, 'WOW', NOW()),
    (7, 1, 'LOVE', NOW()),
    (8, 7, 'LIKE', NOW());

-- =============================
-- Bảng comment_reaction_count
-- =============================
INSERT INTO comment_reaction_count (comment_id, reaction_type, count)
VALUES
    (1, 'LIKE', 1), (1, 'LOVE', 1),
    (2, 'LIKE', 1), (2, 'WOW', 1),
    (3, 'LIKE', 1),
    (4, 'LOVE', 1),
    (5, 'LIKE', 1),
    (6, 'WOW', 1),
    (7, 'LOVE', 1),
    (8, 'LIKE', 1);

-- =============================
-- Bảng post_reaction
-- =============================
INSERT INTO post_reaction (post_id, user_id, reaction_type, created_at)
VALUES
    (1, 2, 'LOVE', NOW()),
    (1, 3, 'LIKE', NOW()),
    (2, 4, 'LIKE', NOW()),
    (3, 1, 'LOVE', NOW()),
    (4, 5, 'WOW', NOW()),
    (5, 6, 'LIKE', NOW()),
    (6, 7, 'LOVE', NOW()),
    (7, 8, 'LIKE', NOW()),
    (8, 9, 'WOW', NOW()),
    (9, 10, 'LIKE', NOW());

-- =============================
-- Bảng post_reaction_count
-- =============================
INSERT INTO post_reaction_count (comment_id, reaction_type, count)
VALUES
    (1, 'LIKE', 1), (1, 'LOVE', 1),
    (2, 'LIKE', 1),
    (3, 'LOVE', 1),
    (4, 'WOW', 1),
    (5, 'LIKE', 1),
    (6, 'LOVE', 1),
    (7, 'LIKE', 1),
    (8, 'WOW', 1),
    (9, 'LIKE', 1);

-- =============================
-- Bảng share
-- =============================
INSERT INTO share (post_id, user_id, platform, created_at)
VALUES
    (1, 2, 'Facebook', NOW()),
    (2, 4, 'Zalo', NOW()),
    (3, 1, 'Messenger', NOW()),
    (4, 5, 'Instagram', NOW()),
    (5, 6, 'Facebook', NOW()),
    (6, 7, 'Zalo', NOW()),
    (7, 8, 'Messenger', NOW()),
    (8, 9, 'Facebook', NOW()),
    (9, 10, 'Instagram', NOW()),
    (10, 3, 'Zalo', NOW());

-- =============================
-- 10 collections cho user_id = 1
-- =============================
INSERT INTO collection (user_id, name, cover_image_url)
VALUES
    (1, 'Món yêu thích', 'https://cdn.monngonviet.vn/bunbohue.jpg'),
    (1, 'Bữa sáng ngon', 'https://cdn.monngonviet.vn/banhmi.jpg'),
    (1, 'Đồ ăn healthy', 'https://cdn.monngonviet.vn/goicuon.jpg'),
    (1, 'Ẩm thực miền Trung', 'https://cdn.monngonviet.vn/banhxeo.jpg'),
    (1, 'Ẩm thực miền Bắc', 'https://cdn.monngonviet.vn/phobo.jpg'),
    (1, 'Ẩm thực miền Nam', 'https://cdn.monngonviet.vn/comtam.jpg'),
    (1, 'Món nước yêu thích', 'https://cdn.monngonviet.vn/soixeo.jpg'),
    (1, 'Món chay thanh đạm', 'https://cdn.monngonviet.vn/goicuon.jpg'),
    (1, 'Tráng miệng nhẹ nhàng', 'https://cdn.monngonviet.vn/banhcuon.jpg'),
    (1, 'Món ăn đặc sản', 'https://cdn.monngonviet.vn/caolau.jpg');

-- Insert 6 posts into Collection 1
INSERT INTO collection_post (collection_id, post_id, created_at) VALUES
   (1, 1, NOW()),
   (1, 2, NOW()),
   (1, 3, NOW()),
   (1, 4, NOW()),
   (1, 5, NOW()),
   (1, 6, NOW());

-- Insert 6 posts into Collection 2
INSERT INTO collection_post (collection_id, post_id, created_at) VALUES
    (2, 7, NOW()),
    (2, 8, NOW()),
    (2, 9, NOW()),
    (2, 10, NOW());

INSERT INTO follow (created_by, is_deleted, updated_at, updated_by, follower_id, following_id)
VALUES
-- User 1 follows users 2, 3, 4
('system', false, now(), 'system', 1, 2),
('system', false, now(), 'system', 1, 3),
('system', false, now(), 'system', 1, 4),

-- User 2 follows users 1, 5, 6
('system', false, now(), 'system', 2, 1),
('system', false, now(), 'system', 2, 5),
('system', false, now(), 'system', 2, 6),

-- User 3 follows users 1, 7, 8
('system', false, now(), 'system', 3, 1),
('system', false, now(), 'system', 3, 7),
('system', false, now(), 'system', 3, 8),

-- User 4 follows users 2, 9, 10
('system', false, now(), 'system', 4, 2),
('system', false, now(), 'system', 4, 9),
('system', false, now(), 'system', 4, 10),

-- User 5 follows users 3, 11, 12
('system', false, now(), 'system', 5, 3),
('system', false, now(), 'system', 5, 11),
('system', false, now(), 'system', 5, 12),

-- User 6 follows users 4, 13, 14
('system', false, now(), 'system', 6, 4),
('system', false, now(), 'system', 6, 13),
('system', false, now(), 'system', 6, 14),

-- User 7 follows users 5, 15, 16
('system', false, now(), 'system', 7, 5),
('system', false, now(), 'system', 7, 15),
('system', false, now(), 'system', 7, 16),

-- User 8 follows users 6, 17, 18
('system', false, now(), 'system', 8, 6),
('system', false, now(), 'system', 8, 17),
('system', false, now(), 'system', 8, 18),

-- User 9 follows users 7, 19, 20
('system', false, now(), 'system', 9, 7),
('system', false, now(), 'system', 9, 19),
('system', false, now(), 'system', 9, 20),

-- User 10 follows users 8, 1, 2
('system', false, now(), 'system', 10, 8),
('system', false, now(), 'system', 10, 1),
('system', false, now(), 'system', 10, 2);