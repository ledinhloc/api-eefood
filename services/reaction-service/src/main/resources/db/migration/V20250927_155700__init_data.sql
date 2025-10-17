-- =============================
-- Bảng post
-- =============================
INSERT INTO post (created_at, updated_at, created_by, updated_by, is_deleted, user_id, recipe_id, title, content, image_url)
VALUES
    (NOW(), NOW(), 'hoanganh', 'hoanganh', false, 1, 101, 'Bún bò Huế', 'Tô bún bò cay nồng chuẩn vị miền Trung', 'https://cdn.monngonviet.vn/bunbohue.jpg'),
    (NOW(), NOW(), 'ngocmai', 'ngocmai', false, 2, 102, 'Cơm tấm Sài Gòn', 'Sườn nướng, bì, chả trứng thơm ngon', 'https://cdn.monngonviet.vn/comtam.jpg'),
    (NOW(), NOW(), 'phuonglinh', 'phuonglinh', false, 3, 103, 'Phở bò Hà Nội', 'Nước dùng trong, thơm mùi quế hồi', 'https://cdn.monngonviet.vn/phobo.jpg'),
    (NOW(), NOW(), 'quanghuy', 'quanghuy', false, 4, 104, 'Bánh mì trứng ốp la', 'Bữa sáng nhanh gọn nhưng đủ chất', 'https://cdn.monngonviet.vn/banhmi.jpg'),
    (NOW(), NOW(), 'lananh', 'lananh', false, 5, 105, 'Gỏi cuốn tôm thịt', 'Món ăn nhẹ, thanh mát, ít dầu mỡ', 'https://cdn.monngonviet.vn/goicuon.jpg'),
    (NOW(), NOW(), 'minhtri', 'minhtri', false, 6, 106, 'Chả giò miền Nam', 'Giòn rụm, nhân thịt băm thơm phức', 'https://cdn.monngonviet.vn/chagio.jpg'),
    (NOW(), NOW(), 'hoangphuc', 'hoangphuc', false, 7, 107, 'Bánh xèo tôm thịt', 'Vỏ vàng giòn, ăn cùng rau sống và nước mắm chua ngọt', 'https://cdn.monngonviet.vn/banhxeo.jpg'),
    (NOW(), NOW(), 'thanhha', 'thanhha', false, 8, 108, 'Cao lầu Hội An', 'Mì vàng dai, thịt xá xíu đậm vị', 'https://cdn.monngonviet.vn/caolau.jpg'),
    (NOW(), NOW(), 'kimthao', 'kimthao', false, 9, 109, 'Bánh cuốn Thanh Trì', 'Bột gạo mỏng mịn, nhân thịt mộc nhĩ', 'https://cdn.monngonviet.vn/banhcuon.jpg'),
    (NOW(), NOW(), 'tuananh', 'tuananh', false, 10, 110, 'Xôi xéo Hà Nội', 'Xôi nếp vàng ươm, thơm hành phi', 'https://cdn.monngonviet.vn/soixeo.jpg');

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
    (1, 'https://cdn.example.com/comments/1/img1.jpg'),
    (1, 'https://cdn.example.com/comments/1/img2.jpg'),
    (3, 'https://cdn.example.com/comments/3/img1.jpg');

-- =============================
--  Bảng comment_videos
-- =============================
INSERT INTO comment_videos (comment_id, video_url)
VALUES
    (2, 'https://cdn.example.com/comments/2/video1.mp4'),
    (5, 'https://cdn.example.com/comments/5/video1.mp4');
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
-- Bảng favorite
-- =============================
INSERT INTO favorite (user_id, tag, post_id, created_at)
VALUES
    (1, 'bún bò', 1, NOW()),
    (2, 'phở', 3, NOW()),
    (3, 'cơm tấm', 2, NOW()),
    (4, 'bánh mì', 4, NOW()),
    (5, 'chả giò', 6, NOW()),
    (6, 'bánh xèo', 7, NOW()),
    (7, 'cao lầu', 8, NOW()),
    (8, 'xôi', 10, NOW()),
    (9, 'bánh cuốn', 9, NOW()),
    (10, 'gỏi cuốn', 5, NOW());

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
--  Bảng share
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
