CREATE TABLE client
(
    id SERIAL PRIMARY KEY,
    nickname CHARACTER VARYING(30) NOT NULL CHECK(nickname !=''),
    hash INTEGER NOT NULL,
    email CHARACTER VARYING(30) UNIQUE CHECK(email !='')
);

CREATE TABLE game
(
    id SERIAL PRIMARY KEY,
	title CHARACTER VARYING(30) NOT NULL UNIQUE CHECK(title !=''),
	price INTEGER NOT NULL
);

CREATE TABLE genre
(
    id SERIAL PRIMARY KEY,
	name CHARACTER VARYING(30) CHECK(name !='') UNIQUE
);

CREATE TABLE subscription_plan
(
    id SERIAL PRIMARY KEY,
	name CHARACTER VARYING(30) CHECK(name !='') UNIQUE,
	price INTEGER NOT NULL
);

CREATE TABLE machine
(
    id SERIAL PRIMARY KEY,
	power_tier INTEGER CHECK(power_tier > 0 AND power_tier < 4)
);

CREATE TABLE owned_game
(
    id SERIAL PRIMARY KEY,
	client_id bigint NOT NULL REFERENCES client(id) ON DELETE CASCADE,
	game_id bigint NOT NULL REFERENCES game(id) ON DELETE CASCADE,
	purchase_date DATE NOT NULL
);

CREATE TABLE game_genre
(
    id SERIAL PRIMARY KEY,
	game_id bigint NOT NULL REFERENCES game(id) ON DELETE CASCADE,
	genre_id bigint NOT NULL REFERENCES genre(id) ON DELETE CASCADE
);

CREATE TABLE installed_game
(
    id SERIAL PRIMARY KEY,
	machine_id bigint NOT NULL REFERENCES machine(id) ON DELETE CASCADE,
	game_id bigint NOT NULL REFERENCES game(id) ON DELETE CASCADE
);

CREATE TABLE client_subscription_plan
(
    id SERIAL PRIMARY KEY,
	client_id bigint NOT NULL REFERENCES client(id) ON DELETE CASCADE,
	subscription_plan_id bigint NOT NULL REFERENCES subscription_plan(id) ON DELETE CASCADE,
	active_from DATE NOT NULL,
	active_to DATE NOT NULL
);

CREATE TABLE machine_usage
(
    id SERIAL PRIMARY KEY,
	owned_game_id bigint NOT NULL REFERENCES owned_game(id) ON DELETE SET NULL,
	machine_id bigint NOT NULL REFERENCES machine(id) ON DELETE CASCADE,
	in_use_from TIMESTAMP NOT NULL,
	in_use_to TIMESTAMP
);

CREATE TABLE available_machine_tier
(
    id SERIAL PRIMARY KEY,
	subscription_plan_id bigint NOT NULL REFERENCES subscription_plan(id) ON DELETE CASCADE,
	machine_id bigint NOT NULL REFERENCES machine(id) ON DELETE CASCADE
);