CREATE TABLE meal_plan (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal TEXT,
    start_date DATE,
    end_date DATE,
    note VARCHAR(500),
    user_health_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_meal_plan_user_id UNIQUE (user_id)
);

CREATE TABLE meal_plan_item (
    id BIGSERIAL PRIMARY KEY,
    meal_plan_id BIGINT NOT NULL,
    plan_date DATE,
    meal_slot VARCHAR(50),
    item_order INTEGER,
    item_source VARCHAR(50),
    recipe_id BIGINT,
    post_id BIGINT,
    custom_meal_name TEXT,
    planned_servings INTEGER,
    actual_servings INTEGER,
    status VARCHAR(20),
    recipe_title_snapshot TEXT,
    image_url_snapshot VARCHAR(500),
    recipe_servings_snapshot INTEGER,
    calories_per_serving_snapshot NUMERIC(10,2),
    protein_per_serving_snapshot NUMERIC(10,2),
    carbs_per_serving_snapshot NUMERIC(10,2),
    fat_per_serving_snapshot NUMERIC(10,2),
    fiber_per_serving_snapshot NUMERIC(10,2),
    sugar_per_serving_snapshot NUMERIC(10,2),
    calcium_per_serving_snapshot NUMERIC(10,2),
    sodium_per_serving_snapshot NUMERIC(10,2),
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meal_plan_item_meal_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plan(id) ON DELETE CASCADE
);

CREATE TABLE meal_plan_item_ingredient (
    id BIGSERIAL PRIMARY KEY,
    meal_plan_item_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity VARCHAR(100),
    unit VARCHAR(50),
    note VARCHAR(255),
    CONSTRAINT fk_meal_plan_item_ingredient_item
        FOREIGN KEY (meal_plan_item_id) REFERENCES meal_plan_item(id) ON DELETE CASCADE
);

CREATE INDEX idx_meal_plan_item_meal_plan_id
    ON meal_plan_item(meal_plan_id);

CREATE INDEX idx_meal_plan_item_plan_date
    ON meal_plan_item(plan_date);

CREATE INDEX idx_meal_plan_item_ingredient_item_id
    ON meal_plan_item_ingredient(meal_plan_item_id);

-- Demo data
INSERT INTO meal_plan (
    id, user_id, goal, start_date, end_date, note, user_health_note, created_at, updated_at
) VALUES (
    1, 1, 'giam can, it duong', DATE '2026-03-24', DATE '2026-03-30',
    'Meal plan demo trong 7 ngay',
    'Di ung tom, uu tien mon it duong va nhieu rau',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO meal_plan_item (
    id, meal_plan_id, plan_date, meal_slot, item_order, item_source,
    recipe_id, post_id, custom_meal_name, planned_servings, actual_servings, status,
    recipe_title_snapshot, image_url_snapshot, recipe_servings_snapshot,
    calories_per_serving_snapshot, protein_per_serving_snapshot, carbs_per_serving_snapshot,
    fat_per_serving_snapshot, fiber_per_serving_snapshot, sugar_per_serving_snapshot,
    calcium_per_serving_snapshot, sodium_per_serving_snapshot, note,
    created_at, updated_at
) VALUES
(
    1, 1, DATE '2026-03-24', 'BREAKFAST', 1, 'RECIPE',
    101, 201, NULL, 1, NULL, 'PLANNED',
    'Yen mach chuoi sua chua', 'https://example.com/oatmeal.jpg', 1,
    320.00, 12.00, 48.00, 8.00, 6.00, 14.00,
    180.00, 95.00, 'Bua sang nhe',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    2, 1, DATE '2026-03-24', 'LUNCH', 1, 'RECIPE',
    102, 202, NULL, 2, NULL, 'PLANNED',
    'Uc ga ap chao voi salad', 'https://example.com/chicken-salad.jpg', 2,
    410.00, 35.00, 18.00, 15.00, 5.00, 4.00,
    70.00, 260.00, 'Tang protein cho bua trua',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    3, 1, DATE '2026-03-24', 'DINNER', 1, 'CUSTOM',
    NULL, NULL, 'Salad ca ngu tu lam', 1, NULL, 'PLANNED',
    NULL, NULL, NULL,
    280.00, 24.00, 12.00, 14.00, 4.00, 3.00,
    60.00, 210.00, 'Mon user tu them',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    4, 1, DATE '2026-03-25', 'SNACK', 1, 'CUSTOM',
    NULL, NULL, 'Sua chua hat chia', 1, 1, 'DONE',
    NULL, NULL, NULL,
    150.00, 6.00, 14.00, 5.00, 4.00, 9.00,
    120.00, 45.00, 'An xong trong bua phu',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO meal_plan_item_ingredient (
    id, meal_plan_item_id, name, quantity, unit, note
) VALUES
    (1, 3, 'Ca ngu', '100', 'g', NULL),
    (2, 3, 'Xa lach', '80', 'g', NULL),
    (3, 3, 'Ca chua bi', '50', 'g', NULL),
    (4, 3, 'Dau oliu', '10', 'ml', 'Them sau cung'),
    (5, 4, 'Sua chua khong duong', '1', 'hop', NULL),
    (6, 4, 'Hat chia', '10', 'g', NULL);

SELECT setval('meal_plan_id_seq', COALESCE((SELECT MAX(id) FROM meal_plan), 1), true);
SELECT setval('meal_plan_item_id_seq', COALESCE((SELECT MAX(id) FROM meal_plan_item), 1), true);
SELECT setval('meal_plan_item_ingredient_id_seq', COALESCE((SELECT MAX(id) FROM meal_plan_item_ingredient), 1), true);
