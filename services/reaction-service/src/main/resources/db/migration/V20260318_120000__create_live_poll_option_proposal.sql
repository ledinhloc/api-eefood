CREATE TABLE IF NOT EXISTS live_poll_option_proposals (
  id              BIGSERIAL PRIMARY KEY,
  poll_id         BIGINT NOT NULL,
  proposed_by     BIGINT NOT NULL,
  text            VARCHAR(200) NOT NULL,
  status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_poll_option_proposals_poll
    ON live_poll_option_proposals (poll_id);

CREATE INDEX IF NOT EXISTS idx_poll_option_proposals_status
    ON live_poll_option_proposals (status);

ALTER TABLE live_poll_option_proposals
    ADD CONSTRAINT fk_live_poll_option_proposals_poll
        FOREIGN KEY (poll_id) REFERENCES live_polls(id)
            ON DELETE CASCADE;