ALTER TABLE meal_plan_item_ingredient
    ALTER COLUMN quantity TYPE DOUBLE PRECISION
    USING CASE
        WHEN quantity IS NULL OR btrim(quantity) = '' THEN NULL
        WHEN replace(btrim(quantity), ',', '.') ~ '^-?[0-9]+(\.[0-9]+)?$'
            THEN replace(btrim(quantity), ',', '.')::DOUBLE PRECISION
        ELSE NULL
    END;
