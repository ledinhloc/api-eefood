DROP INDEX IF EXISTS public.idx_user_height_user_recorded_at;
DROP INDEX IF EXISTS public.idx_user_weight_user_recorded_at;

ALTER TABLE public.user_height
    RENAME COLUMN recorded_at TO recorded_date;

ALTER TABLE public.user_height
    ALTER COLUMN recorded_date TYPE date USING recorded_date::date;

ALTER TABLE public.user_weight
    RENAME COLUMN recorded_at TO recorded_date;

ALTER TABLE public.user_weight
    ALTER COLUMN recorded_date TYPE date USING recorded_date::date;

ALTER TABLE public.user_height
    ADD CONSTRAINT uq_user_height_user_recorded_date UNIQUE (user_id, recorded_date);

ALTER TABLE public.user_weight
    ADD CONSTRAINT uq_user_weight_user_recorded_date UNIQUE (user_id, recorded_date);

CREATE INDEX idx_user_height_user_recorded_date
    ON public.user_height (user_id, recorded_date DESC);

CREATE INDEX idx_user_weight_user_recorded_date
    ON public.user_weight (user_id, recorded_date DESC);
