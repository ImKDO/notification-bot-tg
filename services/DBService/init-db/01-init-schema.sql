CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    id_tg_chat BIGINT UNIQUE NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_id_tg_chat ON users(id_tg_chat);

CREATE TABLE IF NOT EXISTS services (
    id SERIAL PRIMARY KEY,
    link VARCHAR(256) NOT NULL,
    name VARCHAR(256) NOT NULL
);

CREATE TABLE IF NOT EXISTS methods (
    id SERIAL PRIMARY KEY,
    id_service INTEGER NOT NULL,
    name VARCHAR(16) NOT NULL,
    describe VARCHAR(256) NOT NULL,
    CONSTRAINT fk_methods_service FOREIGN KEY (id_service) REFERENCES services(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_methods_id_service ON methods(id_service);

CREATE TABLE IF NOT EXISTS filters (
    id SERIAL PRIMARY KEY,
    id_service INTEGER NOT NULL,
    name VARCHAR(32) NOT NULL,
    describe VARCHAR(256) NOT NULL,
    CONSTRAINT fk_filters_service FOREIGN KEY (id_service) REFERENCES services(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_filters_id_service ON filters(id_service);

CREATE TABLE IF NOT EXISTS tokens (
    id SERIAL PRIMARY KEY,
    value VARCHAR(256) NOT NULL,
    id_tg_chat BIGINT NOT NULL,
    CONSTRAINT fk_tokens_user FOREIGN KEY (id_tg_chat) REFERENCES users(id_tg_chat) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tokens_id_tg_chat ON tokens(id_tg_chat);

CREATE TABLE IF NOT EXISTS actions (
    id SERIAL PRIMARY KEY,
    id_method INTEGER NOT NULL,
    id_token INTEGER,
    id_tg_chat BIGINT NOT NULL,
    id_service INTEGER NOT NULL,
    describe VARCHAR(64),
    query VARCHAR(16384),
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_check_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_actions_method FOREIGN KEY (id_method) REFERENCES methods(id) ON DELETE CASCADE,
    CONSTRAINT fk_actions_token FOREIGN KEY (id_token) REFERENCES tokens(id) ON DELETE CASCADE,
    CONSTRAINT fk_actions_user FOREIGN KEY (id_tg_chat) REFERENCES users(id_tg_chat) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_actions_id_method ON actions(id_method);
CREATE INDEX IF NOT EXISTS idx_actions_id_token ON actions(id_token);
CREATE INDEX IF NOT EXISTS idx_actions_id_tg_chat ON actions(id_tg_chat);
CREATE INDEX IF NOT EXISTS idx_actions_last_check_date ON actions(last_check_date);

CREATE TABLE IF NOT EXISTS tags (
    id SERIAL PRIMARY KEY,
    id_tg_chat BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    CONSTRAINT fk_tags_user FOREIGN KEY (id_tg_chat) REFERENCES users(id_tg_chat) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tags_id_tg_chat ON tags(id_tg_chat);

CREATE TABLE IF NOT EXISTS history_answers (
    id SERIAL PRIMARY KEY,
    id_tg_chat BIGINT NOT NULL,
    response VARCHAR(2048) NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_answers_user FOREIGN KEY (id_tg_chat) REFERENCES users(id_tg_chat) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_history_answers_id_tg_chat ON history_answers(id_tg_chat);

CREATE TABLE IF NOT EXISTS summary_reposts (
    id SERIAL PRIMARY KEY,
    id_tg_chat BIGINT NOT NULL,
    report VARCHAR(4096) NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_summary_reposts_user FOREIGN KEY (id_tg_chat) REFERENCES users(id_tg_chat) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_summary_reposts_id_tg_chat ON summary_reposts(id_tg_chat);

CREATE TABLE IF NOT EXISTS requests_new_services (
    id SERIAL PRIMARY KEY,
    id_tg_chat BIGINT NOT NULL,
    link VARCHAR(256) NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_new_services_user FOREIGN KEY (id_tg_chat) REFERENCES users(id_tg_chat) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_requests_new_services_id_tg_chat ON requests_new_services(id_tg_chat);

INSERT INTO services (link, name) VALUES
    ('https://api.github.com', 'GitHub')
ON CONFLICT DO NOTHING;

INSERT INTO services (link, name) VALUES
    ('https://api.stackexchange.com', 'StackOverflow')
ON CONFLICT DO NOTHING;

INSERT INTO methods (id_service, name, describe) VALUES
    (1, 'ISSUE', 'Track GitHub issues'),
    (1, 'COMMIT', 'Track GitHub commits'),
    (1, 'BRANCH', 'Track GitHub branches'),
    (1, 'PULL_REQUEST', 'Track GitHub pull requests'),
    (1, 'GITHUB_ACTIONS', 'Track GitHub Actions')
ON CONFLICT DO NOTHING;

INSERT INTO methods (id_service, name, describe) VALUES
    (2, 'NEW_ANSWER', 'Track new answers on StackOverflow questions'),
    (2, 'NEW_COMMENT', 'Track new comments on StackOverflow questions')
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    RAISE NOTICE 'Database notification_bot initialized successfully!';
END $$;

