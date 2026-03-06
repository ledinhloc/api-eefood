CREATE TABLE IF NOT EXISTS live_polls (
  id               BIGSERIAL PRIMARY KEY,
  live_stream_id   BIGINT NOT NULL,
  question         VARCHAR(500) NOT NULL,
  status           VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- PollStatus enum (DRAFT/OPEN/CLOSED...)
  opened_at        TIMESTAMP,
  closed_at        TIMESTAMP
);

-- index theo entity: idx_live_polls_livestream(liveStreamId)
CREATE INDEX IF NOT EXISTS idx_live_polls_livestream
    ON live_polls (live_stream_id);


CREATE TABLE IF NOT EXISTS live_poll_options (
 id        BIGSERIAL PRIMARY KEY,
 poll_id   BIGINT NOT NULL,
 text      VARCHAR(200) NOT NULL,
 count     BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_poll_options_poll
    ON live_poll_options (poll_id);

ALTER TABLE live_poll_options
    ADD CONSTRAINT fk_live_poll_options_poll
        FOREIGN KEY (poll_id) REFERENCES live_polls(id)
            ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS live_poll_settings (
  id                BIGSERIAL PRIMARY KEY,
  poll_id            BIGINT NOT NULL,

  allow_change_vote  BOOLEAN NOT NULL DEFAULT FALSE,
  multiple_choice    BOOLEAN NOT NULL DEFAULT FALSE,
  max_choices        INTEGER NOT NULL DEFAULT 1,

  result_visibility  VARCHAR(50) NOT NULL DEFAULT 'AFTER_VOTE', -- PollResultVisibility
  voter_visibility   VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',     -- PollVoterVisibility
  option_add_mode    VARCHAR(50) NOT NULL DEFAULT 'HOST_ONLY',  -- PollOptionAddMode

  created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uk_poll_setting_poll UNIQUE (poll_id)
);

CREATE INDEX IF NOT EXISTS idx_poll_settings_poll
    ON live_poll_settings (poll_id);

ALTER TABLE live_poll_settings
    ADD CONSTRAINT fk_live_poll_settings_poll
        FOREIGN KEY (poll_id) REFERENCES live_polls(id)
            ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS live_poll_votes (
    id         BIGSERIAL PRIMARY KEY,
    poll_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    option_id  BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_poll_user UNIQUE (poll_id, user_id, option_id)
);

CREATE INDEX IF NOT EXISTS idx_votes_poll
    ON live_poll_votes (poll_id);

ALTER TABLE live_poll_votes
    ADD CONSTRAINT fk_live_poll_votes_poll
        FOREIGN KEY (poll_id) REFERENCES live_polls(id)
            ON DELETE CASCADE;

ALTER TABLE live_poll_votes
    ADD CONSTRAINT fk_live_poll_votes_option
        FOREIGN KEY (option_id) REFERENCES live_poll_options(id)
            ON DELETE CASCADE;