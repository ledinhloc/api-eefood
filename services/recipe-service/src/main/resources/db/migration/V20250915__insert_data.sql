-- Insert categories
INSERT INTO categories (created_at, updated_at, created_by, updated_by, is_deleted, icon_url, description)
VALUES
    (now(), now(), 'System', 'System', false, 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg/1920px-C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg', 'Món chính'),
    (now(), now(), 'System', 'System', false, 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg/1920px-C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg', 'Món nước'),
    (now(), now(), 'System', 'System', false, 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg/1920px-C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg', 'Món ăn sáng'),
    (now(), now(), 'System', 'System', false, 'https://suminhchau.com/wp-content/uploads/2023/04/banh-flan-2.jpg', 'Món đường phố'),
    (now(), now(), 'System', 'System', false, 'https://suminhchau.com/wp-content/uploads/2023/04/banh-flan-2.jpg', 'Món tráng miệng'),
    (now(), now(), 'System', 'System', false, null, 'Món cuốn');

-- Insert ingredients
INSERT INTO ingredients (created_at, updated_at, created_by, updated_by, is_deleted, name, description, image)
VALUES
    (now(), now(), 'System', 'System', false, 'Bánh phở', 'Sợi phở mềm', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQGxVTnZ1UTbq6PE4Alaf3Wjs_cD6YqDQ2x2Q&s'),
    (now(), now(), 'System', 'System', false, 'Thịt bò', 'Thịt bò tái hoặc chín', 'https://cdn.tgdd.vn/2021/01/content/bo%CC%80vai-800x500.jpg'),
    (now(), now(), 'System', 'System', false, 'Nước dùng', 'Nước hầm xương bò', 'https://cdn2.fptshop.com.vn/unsafe/1920x0/filters:format(webp):quality(75)/cach_ham_xuong_nhanh_mem_thumb_e223bc76e7.jpg'),
    (now(), now(), 'System', 'System', false, 'Bánh mì', 'Ổ bánh mì Việt Nam', 'https://cdn.tgdd.vn/2020/09/CookProduct/1260-1200x676-52.jpg'),
    (now(), now(), 'System', 'System', false, 'Thịt heo', 'Thịt heo', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLGOwMr44s_OFDGEmEHivFbbFitsGfcg8ILg&s'),
    (now(), now(), 'System', 'System', false, 'Chả lụa', 'Chả lụa cắt lát', 'https://cdn2.fptshop.com.vn/unsafe/1920x0/filters:format(webp):quality(75)/2023_12_6_638374664335050436_cach-bao-quan-cha-lua-thumb.JPG'),
    (now(), now(), 'System', 'System', false, 'Rau sống', 'Xà lách, rau thơm', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTR9FsXSE4ODSxSdg0PuDt3TXdWuXLrB6tS5Q&s'),
    (now(), now(), 'System', 'System', false, 'Nước mắm', 'Nước mắm pha chua ngọt', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSw85u-MiIdALap3ZiqKvbMpHnmjjIVy2qIpA&s'),
    (now(), now(), 'System', 'System', false, 'Bún tươi', 'Bún sợi nhỏ', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSXsLtqGkDVTUaE3Ir7k-0lSY7Kb4qnLFnqkw&s'),
    (now(), now(), 'System', 'System', false, 'Chè thập cẩm', 'Đậu, thạch, sữa dừa', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQwTl7fuuaqG-JzMggFm3xCf4a09zhwqAAD9g&s'),
    (now(), now(), 'System', 'System', false, 'Tôm', 'Tôm tươi bóc vỏ', 'https://cdn.tgdd.vn/Files/2020/08/03/1275265/3-cach-lam-tom-lot-vo-don-gian-nhanh-gon-khong-bi-ra-nuoc-202208111127218761.jpg'),
    (now(), now(), 'System', 'System', false, 'Bột bánh xèo', 'Hỗn hợp bột gạo và nghệ', 'https://cdn.tgdd.vn/2020/06/CookProduct/1200-1200x676-2.jpg'),
    (now(), now(), 'System', 'System', false, 'Mì Cao Lầu', 'Sợi mì vàng đặc trưng Hội An', 'https://cdn.tgdd.vn/2020/10/CookProduct/1200-1200x676-2.jpg'),
    (now(), now(), 'System', 'System', false, 'Đậu xanh', 'Đậu xanh nấu chín nghiền', 'https://cdn.tgdd.vn/Files/2020/09/17/1288693/cach-lam-nhan-dau-xanh-min-muot-deo-ngon-cho-cac-loai-banh-202111171604568423.jpg'),
    (now(), now(), 'System', 'System', false, 'Hành phi', 'Hành khô chiên giòn', 'https://cdn.tgdd.vn/Files/2020/03/17/1243060/huong-dan-cach-lam-hanh-phi-vang-gion-de-dang-thuc-hien-tai-nha-202008261527577817.jpg');

-- Insert recipes
INSERT INTO recipes (created_at, updated_at, created_by, updated_by, is_deleted, author_id, title, description, region, image_url, video_url, prep_time, cook_time, difficulty)
VALUES
    (now(), now(), 'System', 'System', false, 3, 'Phở bò Hà Nội', 'Món phở truyền thống Việt Nam', 'Hà Nội', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ5ffKuq9DTyRpBOg8ZGP01aszcSZcJ9fQXDw&s', null, 30, 120, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 4, 'Bánh mì trứng ốp la', 'Bánh mì kẹp đặc trưng', 'Sài Gòn', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_BEyGNxXtr_7w8z-sVo0SushqWzwSvB6xiQ&s', null, 15, 10, 'EASY'),
    (now(), now(), 'System', 'System', false, 5, 'Gỏi cuốn tôm thịt', 'Cuốn tôm thịt với rau sống', 'Nam Bộ', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT5kk3PGeI2UfstwOYq5tb15E1Bbsn53I6v_Q&s', null, 20, 0, 'EASY'),
    (now(), now(), 'System', 'System', false, 1, 'Bún Chả', 'Bún ăn kèm thịt nướng và nước mắm', 'Hà Nội', 'https://static.vinwonders.com/production/bun-cha-ha-noi-ngon-o-sai-gon-thumb.jpg', null, 30, 30, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 2, 'Cơm tấm Sài Gòn', 'Cơm tấm sườn bì chả', 'Sài Gòn', 'https://file.hstatic.net/1000394081/article/com-tam_e03b4325c9914def9d66619930a73432.jpg', null, 25, 40, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 1, 'Chè Thái', 'Chè trái cây và sữa dừa', 'Nam Bộ', 'https://file.hstatic.net/200000868155/article/1975-post-cach-lam-che-thai-thom-mat-don-gian-tai-nha-1_becf395f6dc248f4bd880d62dc4d4fe0.jpg', null, 15, 0, 'EASY'),
    (now(), now(), 'System', 'System', false, 1, 'Bún bò Huế', 'Món bún cay nồng, đặc trưng vị Huế', 'Miền Trung', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQq6UpPrSe9eYszz1gC0GUFxGepOZvX5hKkIw&s', null, 30, 120, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 6, 'Chả giò miền Nam', 'Món chiên giòn rụm, nhân thịt băm thơm phức', 'Miền Nam', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTZt5emkGhgPt-K26Cq2oqT0HC0B4jtCm7xVQ&s', null, 25, 15, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 7, 'Bánh xèo tôm thịt', 'Vỏ vàng giòn, nhân tôm thịt, ăn cùng rau sống', 'Miền Trung', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTQ2xosMlmOO05CRIspcYSrhvll2HTwnRYBYw&s', null, 30, 20, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 8, 'Cao lầu Hội An', 'Mì vàng dai, thịt xá xíu đậm vị', 'Miền Trung', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQNsIEhWInFKJj4j9XcMgtvL9LNYCWv_kIkLQ&s', null, 25, 35, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 9, 'Bánh cuốn Thanh Trì', 'Món bánh mềm mịn, nhân thịt mộc nhĩ', 'Miền Bắc', 'https://static.vinwonders.com/production/banh-cuon-thanh-tri-banner.jpeg', null, 20, 20, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, 10, 'Xôi xéo Hà Nội', 'Xôi nếp vàng ươm, thơm hành phi', 'Miền Bắc', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTLXHCgHafFLTjysi9B5c1qDkgbYs_ef_qGvw&s', null, 15, 30, 'EASY');
-- Map recipe to categories
INSERT INTO recipe_category (recipe_id, category_id)
VALUES
    (1, 1), (1, 2), (1, 3),          -- Phở bò: Món chính, món nước, món sáng
    (2, 1), (2, 3), (2, 4),          -- Bánh mì: món chính, sáng, đường phố
    (3, 6), (3, 1),                  -- Gỏi cuốn: món cuốn, món chính
    (4, 1), (4, 2), (4, 4),          -- Bún chả: món chính, món nước, đường phố
    (5, 1), (5, 4), (5, 3),          -- Cơm tấm: món chính, sáng, đường phố
    (6, 5),                          -- Chè thái: món tráng miệng
    (7, 2), (7, 1),              -- Bún bò Huế: món nước, món chính
    (8, 1), (8, 4),              -- Chả giò: món chính, đường phố
    (9, 1), (9, 4),              -- Bánh xèo: món chính, đường phố
    (10, 1),                                         -- Cao lầu: món chính
    (11, 3), (11, 1),              -- Bánh cuốn: món sáng, món chính
    (12, 3), (12, 5);            -- Xôi xéo: món sáng, tráng miệng

-- Recipe ingredients
INSERT INTO recipe_ingredients (created_at, updated_at, created_by, updated_by, is_deleted, recipe_id, ingredient_id, quantity, unit)
VALUES
    (now(), now(), 'System', 'System', false, 1, 1, 200, 'gram'),   -- Phở bò: bánh phở
    (now(), now(), 'System', 'System', false, 1, 2, 150, 'gram'),   -- thịt bò
    (now(), now(), 'System', 'System', false, 1, 3, 500, 'ml'),     -- nước dùng

    (now(), now(), 'System', 'System', false, 2, 4, 1, 'ổ'),        -- Bánh mì
    (now(), now(), 'System', 'System', false, 2, 5, 100, 'gram'),   -- thịt heo nướng
    (now(), now(), 'System', 'System', false, 2, 6, 50, 'gram'),    -- chả lụa

    (now(), now(), 'System', 'System', false, 3, 7, 100, 'gram'),   -- rau sống
    (now(), now(), 'System', 'System', false, 3, 5, 50, 'gram'),    -- thịt heo nướng

    (now(), now(), 'System', 'System', false, 4, 9, 200, 'gram'),   -- bún
    (now(), now(), 'System', 'System', false, 4, 5, 100, 'gram'),   -- thịt heo nướng
    (now(), now(), 'System', 'System', false, 4, 8, 50, 'ml'),      -- nước mắm

    (now(), now(), 'System', 'System', false, 5, 5, 150, 'gram'),   -- cơm tấm: thịt heo nướng
    (now(), now(), 'System', 'System', false, 5, 8, 50, 'ml'),      -- nước mắm

    (now(), now(), 'System', 'System', false, 6, 10, 1, 'ly'),      -- chè thái

    (now(), now(), 'System', 'System', false, 7, 9, 200, 'gram'),   -- bún bò Huế: bún
    (now(), now(), 'System', 'System', false, 7, 2, 150, 'gram'),   -- thịt bò
    (now(), now(), 'System', 'System', false, 7, 3, 500, 'ml'),     -- nước dùng

    (now(), now(), 'System', 'System', false, 8, 5, 100, 'gram'),   -- Chả giò: thịt heo
    (now(), now(), 'System', 'System', false, 8, 7, 50, 'gram'),    -- rau sống

    (now(), now(), 'System', 'System', false, 9, 12, 100, 'gram'),  -- Bánh xèo: bột bánh xèo
    (now(), now(), 'System', 'System', false, 9, 11, 50, 'gram'),   -- tôm
    (now(), now(), 'System', 'System', false, 9, 7, 50, 'gram'),    -- rau sống

    (now(), now(), 'System', 'System', false, 10, 13, 150, 'gram'),  -- Cao lầu: mì
    (now(), now(), 'System', 'System', false, 10, 5, 100, 'gram'),   -- thịt heo

    (now(), now(), 'System', 'System', false, 11, 5, 50, 'gram'),    -- Bánh cuốn: thịt heo
    (now(), now(), 'System', 'System', false, 11, 7, 30, 'gram'),    -- rau sống

    (now(), now(), 'System', 'System', false, 12, 14, 200, 'gram'), -- Xôi xéo: đậu xanh
    (now(), now(), 'System', 'System', false, 12, 15, 20, 'gram');  -- hành phi

-- Recipe steps (example shortened)
INSERT INTO recipe_steps
(created_at, updated_at, created_by, updated_by, is_deleted, recipe_id, step_number, instruction, step_time)
VALUES
    (now(), now(), 'System', 'System', false, 1, 1, 'Hầm xương bò để lấy nước dùng', NULL),
    (now(), now(), 'System', 'System', false, 1, 2, 'Trụng bánh phở và bày ra bát', NULL),
    (now(), now(), 'System', 'System', false, 1, 3, 'Thêm thịt bò, chan nước dùng, cho rau thơm', NULL),
    (now(), now(), 'System', 'System', false, 2, 1, 'Nướng thịt heo, chả lụa', NULL),
    (now(), now(), 'System', 'System', false, 2, 2, 'Bỏ vào ổ bánh mì cùng rau thơm', NULL),
    (now(), now(), 'System', 'System', false, 3, 1, 'Chuẩn bị rau sống và thịt', NULL),
    (now(), now(), 'System', 'System', false, 3, 2, 'Cuốn bánh tráng cùng nhân', NULL),
    (now(), now(), 'System', 'System', false, 4, 1, 'Nướng thịt heo', NULL),
    (now(), now(), 'System', 'System', false, 4, 2, 'Ăn cùng bún và nước mắm', NULL),
    (now(), now(), 'System', 'System', false, 5, 1, 'Nướng sườn heo', NULL),
    (now(), now(), 'System', 'System', false, 5, 2, 'Ăn cùng cơm tấm và nước mắm', NULL),
    (now(), now(), 'System', 'System', false, 6, 1, 'Chuẩn bị thạch, trái cây, sữa dừa', NULL),
    (now(), now(), 'System', 'System', false, 6, 2, 'Cho vào ly và thưởng thức', NULL),
    (now(), now(), 'System', 'System', false, 7, 1, 'Hầm xương bò lấy nước dùng', NULL),
    (now(), now(), 'System', 'System', false, 7, 2, 'Nấu bún và nêm gia vị', NULL),
    (now(), now(), 'System', 'System', false, 7, 3, 'Trình bày và thưởng thức', NULL),
    (now(), now(), 'System', 'System', false, 8, 1, 'Cuốn nhân và chiên vàng', NULL),
    (now(), now(), 'System', 'System', false, 9, 1, 'Pha bột bánh xèo và chiên giòn', NULL),
    (now(), now(), 'System', 'System', false, 10, 1, 'Luộc mì cao lầu, xào thịt', NULL),
    (now(), now(), 'System', 'System', false, 11, 1, 'Tráng bánh cuốn, cho nhân thịt', NULL),
    (now(), now(), 'System', 'System', false, 12, 1, 'Đồ xôi, nghiền đậu xanh, rắc hành phi', NULL);


INSERT INTO recipe_step_images (recipe_step_id, image_url)
VALUES
    (1, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTG07ZJ1Hjx4tVG9al90QefJxwEx_7FcXhA7Q&s'),
    (2, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSLGmrUvmPipYnEfu-yfOL4dlPbEMadPmzGuA&s'),
    (3, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQOYwc1mN9pwkHrXFQ-Sdavl5dfTeRRl8Ki-w&s'),
    (4, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRcWhlNGxbRY9V1b8hkNMJUVNrtcXojR7YtcQ&s'),
    (5, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQaenx8VPI70KyxwGaTeeDMBthgW3qjq-gMwA&s'),
    (6, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR3zVyxeM0inddmIoUgwm9zlNS_rz8-H61Lbg&s'),
    (7, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRs8x7dT-Ic7vLrNHBr8KVspi6qonyxTpXQAw&s'),
    (8, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRcWhlNGxbRY9V1b8hkNMJUVNrtcXojR7YtcQ&s'),
    (9, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQgxEa1JTOnVCuABBZIE3s7tM6nvlK9hkkU3A&s'),
    (10, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTr7Hw_D5hSIm9jWsvm7LfcEXvj458i64SHA&s'),
    (11, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQgxEa1JTOnVCuABBZIE3s7tM6nvlK9hkkU3A&s'),
    (12, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSmUcAsrN76gDdbHPIvOMIbwPe9aEPYf_fyXw&s'),
    (13, 'https://cdn2.fptshop.com.vn/unsafe/Uploads/images/tin-tuc/163493/Originals/che-thai-02.jpg'),
    (14, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTG07ZJ1Hjx4tVG9al90QefJxwEx_7FcXhA7Q&s'),
    (15, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQS_VrFctoP6q9_ebkRLGZlCwjS2AjIsV2nzA&s'),
    (16, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRS_-vvxRHP9YX-jRhUz1RGom51LTL3KT-7-Q&s'),
    (17, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTcbtik0wKudnfOfW3eBThuhp_lWOtFMHUUlQ&s'),
    (18, 'https://www.huongnghiepaau.com/wp-content/uploads/2017/02/cong-thuc-pha-bot-banh-xeo.jpg'),
    (19, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR-nKfVkMBR-rPNDk9D9lMwVVMBOv7hqDJ6rQ&s'),
    (20, 'https://i.ytimg.com/vi/rMgPhLjGoS4/maxresdefault.jpg'),
    (21, 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQkuSoJ-s1D2HlXGe8Xf-mBikEKGcoG3LxNsg&s');


