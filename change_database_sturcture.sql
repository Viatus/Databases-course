CREATE TABLE game_price
(
	id SERIAL PRIMARY KEY,
	game_id bigint NOT NULL REFERENCES game(id) ON DELETE CASCADE,
	price INTEGER,
	price_set_date TIMESTAMP,
	price_end_date TIMESTAMP
);

INSERT INTO game_price (game_id) SELECT id from game;
UPDATE game_price SET price = (SELECT price from game WHERE game.id = game_price.game_id);
UPDATE game_price SET price_set_date = current_timestamp;

ALTER TABLE game
DROP price