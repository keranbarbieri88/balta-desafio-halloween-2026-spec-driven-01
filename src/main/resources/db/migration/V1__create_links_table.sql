CREATE TABLE links (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    target_url VARCHAR(2048) NOT NULL,
    created_at_utc TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    expires_at_utc TIMESTAMP WITH TIME ZONE NULL,
    access_count BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_links_code ON links (code);

