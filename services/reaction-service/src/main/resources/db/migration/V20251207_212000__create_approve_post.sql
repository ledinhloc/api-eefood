CREATE TABLE approve_post (
                              id BIGSERIAL PRIMARY KEY,
                              post_id BIGINT NOT NULL,
                              recipe_id BIGINT,
                              user_id BIGINT,
                              status VARCHAR(255),
                              summary TEXT,
                              total_score DOUBLE PRECISION,
                              recipe_completeness INT,
                              ingredient_safety INT,
                              step_clarity INT,
                              content_appropriate INT,
                              content_relevance INT,
                              media_quality INT,
                              completeness_note TEXT,
                              safety_note TEXT,
                              clarity_note TEXT,
                              appropriateness_note TEXT,
                              relevance_note TEXT,
                              media_quality_note TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                              CONSTRAINT fk_post FOREIGN KEY(post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE INDEX idx_approve_post_post_id ON approve_post(post_id);
CREATE INDEX idx_approve_post_created_at ON approve_post(created_at DESC);
