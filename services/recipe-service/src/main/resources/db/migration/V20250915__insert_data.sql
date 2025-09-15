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
    (now(), now(), 'System', 'System', false, 'Bánh phở', 'Sợi phở mềm', null),
    (now(), now(), 'System', 'System', false, 'Thịt bò', 'Thịt bò tái hoặc chín', 'https://lh5.googleusercontent.com/dJ9m3nuT3wFEJSIOlJA8GDlXfyyRZoUjr-NrHlOnjk5p59xfrey02im6q04rcgdV2NV_aTyt6awIzNLT9X-ctm0Pw5XOHzboly5qJO3_mMktRPki_1yGArlM8ieOQPXYr7jb6cFscZ22LDwNjVVWLwY'),
    (now(), now(), 'System', 'System', false, 'Nước dùng', 'Nước hầm xương bò', null),
    (now(), now(), 'System', 'System', false, 'Bánh mì', 'Ổ bánh mì Việt Nam', null),
    (now(), now(), 'System', 'System', false, 'Thịt heo nướng', 'Thịt heo ướp nướng', null),
    (now(), now(), 'System', 'System', false, 'Chả lụa', 'Chả lụa cắt lát', null),
    (now(), now(), 'System', 'System', false, 'Rau sống', 'Xà lách, rau thơm', null),
    (now(), now(), 'System', 'System', false, 'Nước mắm', 'Nước mắm pha chua ngọt', null),
    (now(), now(), 'System', 'System', false, 'Bún tươi', 'Bún sợi nhỏ', null),
    (now(), now(), 'System', 'System', false, 'Chè thập cẩm', 'Đậu, thạch, sữa dừa', null);

-- Insert recipes
INSERT INTO recipes (created_at, updated_at, created_by, updated_by, is_deleted, author_id, title, description, region, image_url, video_url, prep_time, cook_time, difficulty)
VALUES
    (now(), now(), 'System', 'System', false, null, 'Phở Bò', 'Món phở truyền thống Việt Nam', 'Hà Nội', null, null, 30, 120, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, null, 'Bánh Mì', 'Bánh mì kẹp đặc trưng', 'Sài Gòn', null, null, 15, 10, 'EASY'),
    (now(), now(), 'System', 'System', false, null, 'Gỏi Cuốn', 'Cuốn tôm thịt với rau sống', 'Nam Bộ', null, null, 20, 0, 'EASY'),
    (now(), now(), 'System', 'System', false, null, 'Bún Chả', 'Bún ăn kèm thịt nướng và nước mắm', 'Hà Nội', null, null, 30, 30, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, null, 'Cơm Tấm', 'Cơm tấm sườn bì chả', 'Sài Gòn', null, null, 25, 40, 'MEDIUM'),
    (now(), now(), 'System', 'System', false, null, 'Chè Thái', 'Chè trái cây và sữa dừa', 'Nam Bộ', null, null, 15, 0, 'EASY');

-- Map recipe to categories
INSERT INTO recipe_category (recipe_id, category_id)
VALUES
    (1, 1), (1, 2), (1, 3),          -- Phở bò: Món chính, món nước, món sáng
    (2, 1), (2, 3), (2, 4),          -- Bánh mì: món chính, sáng, đường phố
    (3, 6), (3, 1),                  -- Gỏi cuốn: món cuốn, món chính
    (4, 1), (4, 2), (4, 4),          -- Bún chả: món chính, món nước, đường phố
    (5, 1), (5, 4), (5, 3),          -- Cơm tấm: món chính, sáng, đường phố
    (6, 5);                          -- Chè thái: món tráng miệng

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

    (now(), now(), 'System', 'System', false, 6, 10, 1, 'ly');      -- chè thái

-- Recipe steps (example shortened)
INSERT INTO recipe_steps (created_at, updated_at, created_by, updated_by, is_deleted, recipe_id, step_number, instruction)
VALUES
    (now(), now(), 'System', 'System', false, 1, 1, 'Hầm xương bò để lấy nước dùng'),
    (now(), now(), 'System', 'System', false, 1, 2, 'Trụng bánh phở và bày ra bát'),
    (now(), now(), 'System', 'System', false, 1, 3, 'Thêm thịt bò, chan nước dùng, cho rau thơm'),
    (now(), now(), 'System', 'System', false, 2, 1, 'Nướng thịt heo, chả lụa'),
    (now(), now(), 'System', 'System', false, 2, 2, 'Bỏ vào ổ bánh mì cùng rau thơm'),
    (now(), now(), 'System', 'System', false, 3, 1, 'Chuẩn bị rau sống và thịt'),
    (now(), now(), 'System', 'System', false, 3, 2, 'Cuốn bánh tráng cùng nhân'),
    (now(), now(), 'System', 'System', false, 4, 1, 'Nướng thịt heo'),
    (now(), now(), 'System', 'System', false, 4, 2, 'Ăn cùng bún và nước mắm'),
    (now(), now(), 'System', 'System', false, 5, 1, 'Nướng sườn heo'),
    (now(), now(), 'System', 'System', false, 5, 2, 'Ăn cùng cơm tấm và nước mắm'),
    (now(), now(), 'System', 'System', false, 6, 1, 'Chuẩn bị thạch, trái cây, sữa dừa'),
    (now(), now(), 'System', 'System', false, 6, 2, 'Cho vào ly và thưởng thức');
