-- ============================
-- TABLE: live_streams
-- ============================
CREATE TABLE live_streams (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  room_name VARCHAR(255) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  thumbnail_url VARCHAR(500),
  status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
  viewer_count INT NOT NULL DEFAULT 0,
  scheduled_at TIMESTAMP,
  started_at TIMESTAMP,
  ended_at TIMESTAMP,
  livekit_room_sid VARCHAR(255) UNIQUE
);

-- ============================
-- TABLE: live_comment
-- ============================
CREATE TABLE live_comment (
  id BIGSERIAL PRIMARY KEY,
  live_stream_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  message TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(255) NOT NULL,
  updated_by VARCHAR(255) NOT NULL,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_comment_livestream
      FOREIGN KEY (live_stream_id)
          REFERENCES live_streams(id)
);

-- ============================
-- TABLE: live_reaction
-- ============================
CREATE TABLE live_reaction (
   id BIGSERIAL PRIMARY KEY,
   live_stream_id BIGINT NOT NULL,
   user_id BIGINT NOT NULL,
   emotion VARCHAR(50) NOT NULL,
   created_at TIMESTAMP NOT NULL,
   updated_at TIMESTAMP NOT NULL,
   created_by VARCHAR(255) NOT NULL,
   updated_by VARCHAR(255) NOT NULL,
   is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
   CONSTRAINT fk_reaction_livestream
       FOREIGN KEY (live_stream_id)
           REFERENCES live_streams(id)
);

-- ============================
-- TABLE: live_view
-- ============================
CREATE TABLE live_view (
    id BIGSERIAL PRIMARY KEY,
    live_stream_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    left_at TIMESTAMP,
    CONSTRAINT fk_view_livestream
       FOREIGN KEY (live_stream_id)
           REFERENCES live_streams(id)
);

-- ============================
-- INDEXES
-- ============================
CREATE INDEX idx_comment_livestream ON live_comment(live_stream_id);
CREATE INDEX idx_reaction_livestream ON live_reaction(live_stream_id);
CREATE INDEX idx_view_livestream ON live_view(live_stream_id);

CREATE INDEX idx_comment_user ON live_comment(user_id);
CREATE INDEX idx_reaction_user ON live_reaction(user_id);
CREATE INDEX idx_view_user ON live_view(user_id);


-- ========================================
-- INSERT SAMPLE DATA: livestream 1–3
-- ========================================
INSERT INTO live_streams (user_id, room_name, title, status, viewer_count)
VALUES
    (1, 'room-1', 'Livestream nấu ăn số 1', 'SCHEDULED', 0),
    (2, 'room-2', 'Livestream mẹo bếp núc', 'SCHEDULED', 0),
    (3, 'room-3', 'Livestream ẩm thực đường phố', 'SCHEDULED', 0);


-- ========================================
-- INSERT sample comments (user 1 → 20)
-- ========================================
INSERT INTO live_comment (
    live_stream_id, user_id, message, created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
    1, id, CONCAT('Comment từ user ', id),
    NOW(), NOW(),
    'system', 'system', FALSE
FROM generate_series(1,20) AS id;


-- ========================================
-- INSERT sample reactions (user 1 → 20)
-- ========================================
INSERT INTO live_reaction (
    live_stream_id, user_id, emotion, created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
    1, id, 'DELICIOUS',
    NOW(), NOW(),
    'system', 'system', FALSE
FROM generate_series(1,20) AS id;


-- ========================================
-- INSERT sample views (user 1 → 20)
-- ========================================
INSERT INTO live_view (
    live_stream_id, user_id, joined_at, left_at
)
SELECT
    1, id, NOW(), NULL
FROM generate_series(1,20) AS id;
