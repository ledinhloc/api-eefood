CREATE TABLE shopping_items (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id),
    servings INT NOT NULL DEFAULT 1
);

CREATE TABLE shopping_ingredients (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    shopping_item_id BIGINT NOT NULL REFERENCES shopping_items(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    quantity INT NOT NULL,
    unit VARCHAR(50) NOT NULL,
    purchased BOOLEAN NOT NULL DEFAULT false
);

-- Insert shopping_items cho user_id = 1
INSERT INTO shopping_items (created_at, updated_at, created_by, updated_by, is_deleted, user_id, recipe_id, servings)
VALUES
    (now(), now(), 'System', 'System', false, 1, 1, 2),  -- Phở bò
    (now(), now(), 'System', 'System', false, 1, 2, 1),  -- Bánh mì
    (now(), now(), 'System', 'System', false, 1, 3, 3),  -- Gỏi cuốn
    (now(), now(), 'System', 'System', false, 1, 4, 2),  -- Bún chả
    (now(), now(), 'System', 'System', false, 1, 5, 4),  -- Cơm tấm
    (now(), now(), 'System', 'System', false, 1, 6, 2),  -- Chè Thái
    (now(), now(), 'System', 'System', false, 1, 1, 1);  -- Thêm Phở bò (demo thêm 1 item)


-- Phở bò (shopping_item_id = 1)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 1, 1, 400, 'gram', false),  -- Bánh phở
    (now(), now(), 'System', 'System', false, 1, 2, 300, 'gram', false),  -- Thịt bò
    (now(), now(), 'System', 'System', false, 1, 3, 1000, 'ml', false);   -- Nước dùng

-- Bánh mì (shopping_item_id = 2)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 2, 4, 1, 'ổ', false),
    (now(), now(), 'System', 'System', false, 2, 5, 100, 'gram', false),
    (now(), now(), 'System', 'System', false, 2, 6, 50, 'gram', false);

-- Gỏi cuốn (shopping_item_id = 3)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 3, 7, 300, 'gram', false),
    (now(), now(), 'System', 'System', false, 3, 5, 150, 'gram', false);

-- Bún chả (shopping_item_id = 4)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 4, 9, 400, 'gram', false),
    (now(), now(), 'System', 'System', false, 4, 5, 200, 'gram', false),
    (now(), now(), 'System', 'System', false, 4, 8, 100, 'ml', false);

-- Cơm tấm (shopping_item_id = 5)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 5, 5, 600, 'gram', false),
    (now(), now(), 'System', 'System', false, 5, 8, 200, 'ml', false);

-- Chè Thái (shopping_item_id = 6)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 6, 10, 2, 'ly', false);

-- Phở bò lần 2 (shopping_item_id = 7)
INSERT INTO shopping_ingredients (created_at, updated_at, created_by, updated_by, is_deleted,
                                  shopping_item_id, ingredient_id, quantity, unit, purchased)
VALUES
    (now(), now(), 'System', 'System', false, 7, 1, 200, 'gram', false),
    (now(), now(), 'System', 'System', false, 7, 2, 150, 'gram', false),
    (now(), now(), 'System', 'System', false, 7, 3, 500, 'ml', false);