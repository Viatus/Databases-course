INSERT INTO client (nickname, hash, email)
VALUES ('example_client', 5, 'example1@mail.ru'),
('new_client', 28, 'new@gmail.ru');

INSERT INTO game (title, price)
VALUES ('DOOM', 2000),
('Mario', 4500),
('Trails of cold steel', 1000);

INSERT INTO genre (name)
VALUES ('action'),
('arcade'),
('rpg');

INSERT INTO machine (power_tier)
VALUES (1),
(2),
(3);

INSERT INTO subscription_plan (name, price)
VALUES ('low-tier plan', 100),
('middle-tier plan', 200),
('high-tier plan', 300);

INSERT INTO available_machine_tier (subscription_plan_id, machine_id)
VALUES ((SELECT id from subscription_plan WHERE name = 'low-tier plan'), (SELECT id from machine WHERE power_tier = 1)),
((SELECT id from subscription_plan WHERE name = 'middle-tier plan'), (SELECT id from machine WHERE power_tier = 2)),
((SELECT id from subscription_plan WHERE name = 'high-tier plan'), (SELECT id from machine WHERE power_tier = 3));

INSERT INTO client_subscription_plan (client_id, subscription_plan_id, active_from, active_to)
VALUES ((SELECT id from client WHERE email = 'example1@mail.ru'), (SELECT id from subscription_plan WHERE name = 'low-tier plan'), '2019-06-01', '2020-06-01'),
((SELECT id from client WHERE email = 'new@gmail.ru'), (SELECT id from subscription_plan WHERE name = 'middle-tier plan'), '2019-04-02', '2020-04-02');

INSERT INTO game_genre (game_id, genre_id)
VALUES ((SELECT id from game WHERE title = 'DOOM'), (SELECT id from genre WHERE name = 'action')),
((SELECT id from game WHERE title = 'Mario'), (SELECT id from genre WHERE name = 'arcade')),
((SELECT id from game WHERE title = 'Trails of cold steel'), (SELECT id from genre WHERE name = 'rpg'));

INSERT INTO installed_game (machine_id, game_id)
VALUES ((SELECT id from machine WHERE power_tier = 3), (SELECT id from game WHERE title = 'DOOM')),
((SELECT id from machine WHERE power_tier = 1), (SELECT id from game WHERE title = 'Mario')),
((SELECT id from machine WHERE power_tier = 2), (SELECT id from game WHERE title = 'Trails of cold steel'));

INSERT INTO owned_game (client_id, game_id, purchase_date)
VALUES ((SELECT id from client WHERE email = 'example1@mail.ru'), (SELECT id from game WHERE title = 'DOOM'), '2019-06-02'),
((SELECT id from client WHERE email = 'new@gmail.ru'), (SELECT id from game WHERE title = 'Mario'), '2019-07-12'),
((SELECT id from client WHERE email = 'new@gmail.ru'), (SELECT id from game WHERE title = 'Trails of cold steel'), '2019-08-27');

INSERT INTO machine_usage (owned_game_id, machine_id, in_use_from, in_use_to)
VALUES ((SELECT id from owned_game WHERE client_id = (SELECT id from client WHERE email = 'example1@mail.ru') AND game_id = (SELECT id from game WHERE title = 'DOOM')), (SELECT id from machine WHERE power_tier = 3), '2019-07-22 19:10:25-07', '2019-07-22 23:20:25-02'),
((SELECT id from owned_game WHERE client_id = (SELECT id from client WHERE email = 'new@gmail.ru') AND game_id = (SELECT id from game WHERE title = 'Mario')), (SELECT id from machine WHERE power_tier = 1), '2019-10-02 12:00:25-07', '2019-10-02 23:20:25-02'),
((SELECT id from owned_game WHERE client_id = (SELECT id from client WHERE email = 'new@gmail.ru') AND game_id = (SELECT id from game WHERE title = 'Trails of cold steel')), (SELECT id from machine WHERE power_tier = 2), '2019-11-13 22:54:23-07', '2019-11-13 23:42:15-01');