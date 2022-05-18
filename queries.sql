--Сделайте выборку данных из каждой таблицы
SELECT * FROM available_machine_tier;
SELECT * FROM client;
SELECT * FROM client_subscription_plan;
SELECT * FROM game;
SELECT * FROM game_genre;
SELECT * FROM genre;
SELECT * FROM installed_game;
SELECT * FROM machine;
SELECT * FROM machine_usage;
SELECT * FROM owned_game;
SELECT * FROM subscription_plan;
SELECT * FROM game_price;

--Сделайте выборку данных из одной таблицы при нескольких условиях, с использованием логических операций, LIKE, BETWEEN, IN (не менее 3-х разных примеров)
SELECT title FROM game WHERE title like 'Trails%';
SELECT * FROM game_price WHERE price BETWEEN 1000 AND 3000;
SELECT * FROM game WHERE title like 'Trails%' AND (SELECT price FROM game_price WHERE game.id = game_id) in (1000, 2000, 3000);

--Создайте в запросе вычисляемое поле
SELECT MAX(price) FROM game_price;
SELECT AVG(price) FROM game_price WHERE price_end_date IS NULL;
SELECT COUNT(in_use_FROM) FROM machine_usage WHERE in_use_FROM BETWEEN '2019-07-21' AND '2019-12-1';

--Сделайте выборку всех данных с сортировкой по нескольким полям
SELECT * FROM machine_usage
ORDER BY in_use_FROM DESC, in_use_to ASC;
SELECT * FROM client
ORDER BY nickname ASC, email DESC;

--Создайте запрос, вычисляющий несколько совокупных характеристик таблиц
SELECT AVG(price) AS avg_price, MIN(price) AS min_price, MAX(price) AS max_price FROM game_price WHERE price_end_date IS NULL;

--Сделайте выборку данных из связанных таблиц (не менее двух примеров)
SELECT game.title, game_price.price
	FROM game
	INNER JOIN game_price ON game.id = game_price.game_id;
		
SELECT client.nickname, game.title
	FROM owned_game
	INNER JOIN client ON  owned_game.client_id = client.id
	INNER JOIN game ON owned_game.game_id = game.id;

--Создайте запрос, рассчитывающий совокупную характеристику с использованием группировки, наложите ограничение на результат группировки
SELECT client.nickname, COUNT(game.title)
	FROM owned_game
	INNER JOIN client ON  owned_game.client_id = client.id
	INNER JOIN game ON owned_game.game_id = game.id
	GROUP BY nickname HAVING COUNT(title) >= 1
	ORDER BY COUNT(title) DESC;

--Придумайте и реализуйте пример использования вложенного запроса
SELECT * FROM machine_usage
	WHERE owned_game_id IN (SELECT id FROM owned_game
							WHERE game_id in (SELECT game_id FROM game_genre
											  WHERE genre_id = (SELECT id FROM genre
																WHERE name = 'action')));

--С помощью оператора INSERT добавьте в каждую таблицу по одной записи
INSERT INTO client (nickname, hash, email)
	VALUES ('new_client', 33, 'new_cl@gmail.ru');
INSERT INTO game (title)
	VALUES ('new_game');
INSERT INTO genre (name)
	VALUES ('new_genre');
INSERT INTO machine (power_tier)
	VALUES (1);
INSERT INTO subscription_plan (name, price)
	VALUES ('premium-tier plan', 1000);
INSERT INTO available_machine_tier (subscription_plan_id, machine_id)
	VALUES ((SELECT id from subscription_plan WHERE name = 'premium-tier plan'), (SELECT id from machine WHERE power_tier = 3));
INSERT INTO client_subscription_plan (client_id, subscription_plan_id, active_from, active_to)
	VALUES ((SELECT id from client WHERE email = 'new_cl@gmail.ru'), (SELECT id from subscription_plan WHERE name = 'low-tier plan'), '2019-07-21', '2020-07-21');
INSERT INTO game_genre (game_id, genre_id)
	VALUES ((SELECT id from game WHERE title = 'new_game'), (SELECT id from genre WHERE name = 'new_genre'));
INSERT INTO installed_game (machine_id, game_id)
	VALUES ((SELECT id from machine WHERE power_tier = 3 LIMIT 1), (SELECT id from game WHERE title = 'new_game'));
INSERT INTO owned_game (client_id, game_id, purchase_date)
	VALUES ((SELECT id from client WHERE email = 'new_cl@gmail.ru'), (SELECT id from game WHERE title = 'new_game'), '2019-07-03');
INSERT INTO machine_usage (owned_game_id, machine_id, in_use_from, in_use_to)
	VALUES ((SELECT id from owned_game WHERE client_id = (SELECT id from client WHERE email = 'new_cl@gmail.ru') AND game_id = (SELECT id from game WHERE title = 'new_game')), (SELECT id from machine WHERE power_tier = 3 LIMIT 1), '2019-08-12 19:10:25-07', '2019-08-12 23:20:25-02');
INSERT INTO game_price (game_id, price, price_set_date, price_end_date)
	VALUES ((SELECT id from game WHERE title = 'new_game'), 1000, '2015-01-12 00:00:00-00', null);

--С помощью оператора UPDATE измените значения нескольких полей у всех записей, отвечающих заданному условию
UPDATE client
	SET nickname = 'changed_nickname', hash = 111111
		WHERE email = 'new_cl@gmail.ru';
UPDATE machine_usage
	SET in_use_from = in_use_from + INTERVAL'1 hour', in_use_to = in_use_to + INTERVAL'1 hour'
		WHERE machine_id = 3;

--С помощью оператора DELETE удалите запись, имеющую максимальное (минимальное) значение некоторой совокупной характеристики
DELETE FROM machine WHERE id = (SELECT machine_id
								FROM (SELECT machine_id, count(*) AS usage_count 
									  FROM machine_usage
									  GROUP BY machine_id
									  ORDER BY usage_count ASC LIMIT 1) AS least_used_machine_id);
	
DELETE FROM machine WHERE id = (SELECT machine_id
								FROM (SELECT machine_id, MIN(usage_count)
									  FROM (SELECT machine_id, count(*) AS usage_count
									  			FROM machine_usage 
									  			GROUP BY machine_id) AS usage_counted
									  GROUP BY machine_id ) AS least_used_id);

--С помощью оператора DELETE удалите записи в главной таблице, на которые не ссылается подчиненная таблица (используя вложенный запрос)
DELETE FROM game
	WHERE id NOT IN (SELECT DISTINCT game_id
					 FROM game_price);

DELETE FROM machine
	WHERE id NOT IN (SELECT DISTINCT machine_ID
					FROM available_machine_tier);
