
ALTER TABLE actions ALTER COLUMN id_token DROP NOT NULL;

INSERT INTO services (link, name) VALUES
    ('https://api.stackexchange.com', 'StackOverflow')
ON CONFLICT DO NOTHING;

INSERT INTO methods (id_service, name, describe) VALUES
    ((SELECT id FROM services WHERE name = 'StackOverflow'), 'NEW_ANSWER', 'Track new answers on StackOverflow questions'),
    ((SELECT id FROM services WHERE name = 'StackOverflow'), 'NEW_COMMENT', 'Track new comments on StackOverflow questions')
ON CONFLICT DO NOTHING;
