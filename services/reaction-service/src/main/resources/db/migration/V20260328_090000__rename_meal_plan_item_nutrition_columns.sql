ALTER TABLE meal_plan_item
    RENAME COLUMN calories_per_serving_snapshot TO calories;

ALTER TABLE meal_plan_item
    RENAME COLUMN recipe_title_snapshot TO recipe_title;

ALTER TABLE meal_plan_item
    RENAME COLUMN image_url_snapshot TO image_url;

ALTER TABLE meal_plan_item
    RENAME COLUMN protein_per_serving_snapshot TO protein;

ALTER TABLE meal_plan_item
    RENAME COLUMN carbs_per_serving_snapshot TO carbs;

ALTER TABLE meal_plan_item
    RENAME COLUMN fat_per_serving_snapshot TO fat;

ALTER TABLE meal_plan_item
    RENAME COLUMN fiber_per_serving_snapshot TO fiber;

ALTER TABLE meal_plan_item
    RENAME COLUMN sugar_per_serving_snapshot TO sugar;

ALTER TABLE meal_plan_item
    RENAME COLUMN calcium_per_serving_snapshot TO calcium;

ALTER TABLE meal_plan_item
    RENAME COLUMN sodium_per_serving_snapshot TO sodium;

ALTER TABLE meal_plan_item
    DROP COLUMN recipe_servings_snapshot;
