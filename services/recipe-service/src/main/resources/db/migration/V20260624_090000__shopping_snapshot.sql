ALTER TABLE shopping_items
    ALTER COLUMN recipe_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS recipe_title_snapshot VARCHAR(255);

UPDATE shopping_items si
SET recipe_title_snapshot = r.title
FROM recipes r
WHERE si.recipe_id = r.id
  AND si.recipe_title_snapshot IS NULL;

ALTER TABLE shopping_ingredients
    ALTER COLUMN ingredient_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS ingredient_name_snapshot VARCHAR(255);

UPDATE shopping_ingredients spi
SET ingredient_name_snapshot = i.name
FROM ingredients i
WHERE spi.ingredient_id = i.id
  AND spi.ingredient_name_snapshot IS NULL;
