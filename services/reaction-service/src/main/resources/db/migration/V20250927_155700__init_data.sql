
-- =============================
-- Bảng post (CẬP NHẬT với đầy đủ thông tin recipe)
-- =============================
INSERT INTO post (created_at, updated_at, created_by, updated_by, is_deleted, user_id, recipe_id, title, description, region, image_url, prep_time, cook_time, difficulty, content)
VALUES
    -- Post 1: Bún bò Huế (recipe_id = 7)
    (NOW(), NOW(), 'hoanganh', 'hoanganh', false, 1, 7,
     'Bún bò Huế',
     'Món bún cay nồng, đặc trưng vị Huế',
     'Miền Trung',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq6UpPrSe9eYszz1gC0GUFxGepOZvX5hKkIw&s',
     30, 120, 'MEDIUM',
     'Tô bún bò cay nồng chuẩn vị miền Trung'),

    -- Post 2: Cơm tấm Sài Gòn (recipe_id = 5)
    (NOW(), NOW(), 'ngocmai', 'ngocmai', false, 2, 5,
     'Cơm tấm Sài Gòn',
     'Cơm tấm sườn bì chả',
     'Sài Gòn',
     'https://file.hstatic.net/1000394081/article/com-tam_e03b4325c9914def9d66619930a73432.jpg',
     25, 40, 'MEDIUM',
     'Sườn nướng, bì, chả trứng thơm ngon'),

    -- Post 3: Phở bò Hà Nội (recipe_id = 1)
    (NOW(), NOW(), 'phuonglinh', 'phuonglinh', false, 3, 1,
     'Phở bò Hà Nội',
     'Món phở truyền thống Việt Nam',
     'Hà Nội',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ5ffKuq9DTyRpBOg8ZGP01aszcSZcJ9fQXDw&s',
     30, 120, 'MEDIUM',
     'Nước dùng trong, thơm mùi quế hồi'),

    -- Post 4: Bánh mì trứng ốp la (recipe_id = 2)
    (NOW(), NOW(), 'quanghuy', 'quanghuy', false, 4, 2,
     'Bánh mì trứng ốp la',
     'Bánh mì kẹp đặc trưng',
     'Sài Gòn',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_BEyGNxXtr_7w8z-sVo0SushqWzwSvB6xiQ&s',
     15, 10, 'EASY',
     'Bữa sáng nhanh gọn nhưng đủ chất'),

    -- Post 5: Gỏi cuốn tôm thịt (recipe_id = 3)
    (NOW(), NOW(), 'lananh', 'lananh', false, 5, 3,
     'Gỏi cuốn tôm thịt',
     'Cuốn tôm thịt với rau sống',
     'Nam Bộ',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT5kk3PGeI2UfstwOYq5tb15E1Bbsn53I6v_Q&s',
     20, 0, 'EASY',
     'Món ăn nhẹ, thanh mát, ít dầu mỡ'),

    -- Post 6: Chả giò miền Nam (recipe_id = 8)
    (NOW(), NOW(), 'minhtri', 'minhtri', false, 6, 8,
     'Chả giò miền Nam',
     'Món chiên giòn rụm, nhân thịt băm thơm phức',
     'Miền Nam',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTZt5emkGhgPt-K26Cq2oqT0HC0B4jtCm7xVQ&s',
     25, 15, 'MEDIUM',
     'Giòn rụm, nhân thịt băm thơm phức'),

    -- Post 7: Bánh xèo tôm thịt (recipe_id = 9)
    (NOW(), NOW(), 'hoangphuc', 'hoangphuc', false, 7, 9,
     'Bánh xèo tôm thịt',
     'Vỏ vàng giòn, nhân tôm thịt, ăn cùng rau sống',
     'Miền Trung',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTQ2xosMlmOO05CRIspcYSrhvll2HTwnRYBYw&s',
     30, 20, 'MEDIUM',
     'Vỏ vàng giòn, ăn cùng rau sống và nước mắm chua ngọt'),

    -- Post 8: Cao lầu Hội An (recipe_id = 10)
    (NOW(), NOW(), 'thanhha', 'thanhha', false, 8, 10,
     'Cao lầu Hội An',
     'Mì vàng dai, thịt xá xíu đậm vị',
     'Miền Trung',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQNsIEhWInFKJj4j9XcMgtvL9LNYCWv_kIkLQ&s',
     25, 35, 'MEDIUM',
     'Mì vàng dai, thịt xá xíu đậm vị'),

    -- Post 9: Bánh cuốn Thanh Trì (recipe_id = 11)
    (NOW(), NOW(), 'kimthao', 'kimthao', false, 9, 11,
     'Bánh cuốn Thanh Trì',
     'Món bánh mềm mịn, nhân thịt mộc nhĩ',
     'Miền Bắc',
     'https://static.vinwonders.com/production/banh-cuon-thanh-tri-banner.jpeg',
     20, 20, 'MEDIUM',
     'Bột gạo mỏng mịn, nhân thịt mộc nhĩ'),

    -- Post 10: Xôi xéo Hà Nội (recipe_id = 12)
    (NOW(), NOW(), 'tuananh', 'tuananh', false, 10, 12,
     'Xôi xéo Hà Nội',
     'Xôi nếp vàng ươm, thơm hành phi',
     'Miền Bắc',
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTLXHCgHafFLTjysi9B5c1qDkgbYs_ef_qGvw&s',
     15, 30, 'EASY',
     'Xôi nếp vàng ươm, thơm hành phi');

-- =============================
-- Bảng post_recipe_categories
-- Trích xuất từ recipe_category table
-- =============================
-- Post 1: Bún bò Huế (recipe_id = 7) -> categories: 2 (Món nước), 1 (Món chính)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (1, 'Món nước'),
    (1, 'Món chính');

-- Post 2: Cơm tấm (recipe_id = 5) -> categories: 1 (Món chính), 4 (Món đường phố), 3 (Món ăn sáng)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (2, 'Món chính'),
    (2, 'Món đường phố'),
    (2, 'Món ăn sáng');

-- Post 3: Phở bò (recipe_id = 1) -> categories: 1 (Món chính), 2 (Món nước), 3 (Món ăn sáng)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (3, 'Món chính'),
    (3, 'Món nước'),
    (3, 'Món ăn sáng');

-- Post 4: Bánh mì (recipe_id = 2) -> categories: 1 (Món chính), 3 (Món ăn sáng), 4 (Món đường phố)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (4, 'Món chính'),
    (4, 'Món ăn sáng'),
    (4, 'Món đường phố');

-- Post 5: Gỏi cuốn (recipe_id = 3) -> categories: 6 (Món cuốn), 1 (Món chính)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (5, 'Món cuốn'),
    (5, 'Món chính');

-- Post 6: Chả giò (recipe_id = 8) -> categories: 1 (Món chính), 4 (Món đường phố)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (6, 'Món chính'),
    (6, 'Món đường phố');

-- Post 7: Bánh xèo (recipe_id = 9) -> categories: 1 (Món chính), 4 (Món đường phố)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (7, 'Món chính'),
    (7, 'Món đường phố');

-- Post 8: Cao lầu (recipe_id = 10) -> categories: 1 (Món chính)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (8, 'Món chính');

-- Post 9: Bánh cuốn (recipe_id = 11) -> categories: 3 (Món ăn sáng), 1 (Món chính)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (9, 'Món ăn sáng'),
    (9, 'Món chính');

-- Post 10: Xôi xéo (recipe_id = 12) -> categories: 3 (Món ăn sáng), 5 (Món tráng miệng)
INSERT INTO post_recipe_categories (post_id, category)
VALUES
    (10, 'Món ăn sáng'),
    (10, 'Món tráng miệng');

-- =============================
-- Bảng post_recipe_ingredient_keywords
-- Trích xuất từ recipe_ingredients -> ingredients
-- =============================
-- Post 1: Bún bò Huế (recipe_id = 7) -> ingredients: Bún tươi, Thịt bò, Nước dùng
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (1, 'Bún tươi'),
    (1, 'Thịt bò'),
    (1, 'Nước dùng');

-- Post 2: Cơm tấm (recipe_id = 5) -> ingredients: Thịt heo, Nước mắm
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (2, 'Thịt heo'),
    (2, 'Nước mắm');

-- Post 3: Phở bò (recipe_id = 1) -> ingredients: Bánh phở, Thịt bò, Nước dùng
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (3, 'Bánh phở'),
    (3, 'Thịt bò'),
    (3, 'Nước dùng');

-- Post 4: Bánh mì (recipe_id = 2) -> ingredients: Bánh mì, Thịt heo, Chả lụa
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (4, 'Bánh mì'),
    (4, 'Thịt heo'),
    (4, 'Chả lụa');

-- Post 5: Gỏi cuốn (recipe_id = 3) -> ingredients: Rau sống, Thịt heo
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (5, 'Rau sống'),
    (5, 'Thịt heo');

-- Post 6: Chả giò (recipe_id = 8) -> ingredients: Thịt heo, Rau sống
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (6, 'Thịt heo'),
    (6, 'Rau sống');

-- Post 7: Bánh xèo (recipe_id = 9) -> ingredients: Bột bánh xèo, Tôm, Rau sống
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (7, 'Bột bánh xèo'),
    (7, 'Tôm'),
    (7, 'Rau sống');

-- Post 8: Cao lầu (recipe_id = 10) -> ingredients: Mì Cao Lầu, Thịt heo
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (8, 'Mì Cao Lầu'),
    (8, 'Thịt heo');

-- Post 9: Bánh cuốn (recipe_id = 11) -> ingredients: Thịt heo, Rau sống
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (9, 'Thịt heo'),
    (9, 'Rau sống');

-- Post 10: Xôi xéo (recipe_id = 12) -> ingredients: Đậu xanh, Hành phi
INSERT INTO post_recipe_ingredient_keywords (post_id, ingredient_keyword)
VALUES
    (10, 'Đậu xanh'),
    (10, 'Hành phi');

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

INSERT INTO follow (follower_id, following_id, created_at) VALUES
-- User 1 follows 2, 3, 4
(1, 2, now()),
(1, 3, now()),
(1, 4, now()),

-- User 2 follows 1, 5, 6
(2, 1, now()),
(2, 5, now()),
(2, 6, now()),

-- User 3 follows 1, 7, 8
(3, 1, now()),
(3, 7, now()),
(3, 8, now()),

-- User 4 follows 2, 9, 10
(4, 2, now()),
(4, 9, now()),
(4, 10, now()),

-- User 5 follows 3, 11, 12
(5, 3, now()),
(5, 11, now()),
(5, 12, now()),

-- User 6 follows 4, 13, 14
(6, 4, now()),
(6, 13, now()),
(6, 14, now()),

-- User 7 follows 5, 15, 16
(7, 5, now()),
(7, 15, now()),
(7, 16, now()),

-- User 8 follows 6, 17, 18
(8, 6, now()),
(8, 17, now()),
(8, 18, now()),

-- User 9 follows 7, 19, 20
(9, 7, now()),
(9, 19, now()),
(9, 20, now()),

-- User 10 follows 8, 1, 2
(10, 8, now()),
(10, 1, now()),
(10, 2, now());
