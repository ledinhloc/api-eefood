CREATE TABLE IF NOT EXISTS live_stream_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocked_user_id BIGINT NOT NULL,
    streamer_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_live_stream_blocks_streamer
    ON live_stream_blocks (streamer_id);

INSERT INTO live_stream_blocks (blocked_user_id, streamer_id)
VALUES
    (3, 1),
    (4, 1),
    (5, 1),
    (6, 2),
    (7, 2),
    (8, 2),
    (9, 2),
    (10, 2);