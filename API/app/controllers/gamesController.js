import dbQuery from '../db/dev/dbQuery';

import {
	errorMessage, successMessage, status,
} from '../helpers/status';
import validator from 'validator';


const getAllGames = async (req, res) => {
	var getAllGamesQuery
	if (req.query.sort === undefined) {
		getAllGamesQuery = 'SELECT game.id, game.title, game_price.price FROM game INNER JOIN game_price ON game.id = game_price.id AND game_price.price_end_date IS NULL ORDER BY title ASC';
	} else {
		if (req.query.sort == "title" || req.query.sort == "price") {
			if (req.query.order === undefined) {
				getAllGamesQuery = `SELECT game.id, game.title, game_price.price FROM game INNER JOIN game_price ON game.id = game_price.id AND game_price.price_end_date IS NULL ORDER BY ${req.query.sort} ASC`;
			} else {
				getAllGamesQuery = `SELECT game.id, game.title, game_price.price FROM game INNER JOIN game_price ON game.id = game_price.id AND game_price.price_end_date IS NULL ORDER BY ${req.query.sort} ${req.query.order}`;
			}
		} else {
			if (req.query.sort == "popularity") {
				if (req.query.order === undefined) {
					getAllGamesQuery = `SELECT game.title as title,game_price.price as price, SUM((date_part('hour', machine_usage.in_use_to) * 60 + date_part('minute', machine_usage.in_use_to)
				- date_part('hour', machine_usage.in_use_from) * 60 - date_part('minute', machine_usage.in_use_from))) as minutes_played
			FROM owned_game
			RIGHT JOIN machine_usage ON owned_game.id = machine_usage.owned_game_id
			INNER JOIN game ON owned_game.game_id = game.id
			INNER JOIN game_price ON owned_game.game_id = game_price.game_id AND game_price.price_end_date is NULL
			GROUP BY title, price
			ORDER BY minutes_played DESC`
				} else {
					getAllGamesQuery = `SELECT game.title as title,game_price.price as price, SUM((date_part('hour', machine_usage.in_use_to) * 60 + date_part('minute', machine_usage.in_use_to)
				- date_part('hour', machine_usage.in_use_from) * 60 - date_part('minute', machine_usage.in_use_from))) as minutes_played
			FROM owned_game
			RIGHT JOIN machine_usage ON owned_game.id = machine_usage.owned_game_id
			INNER JOIN game ON owned_game.game_id = game.id
			INNER JOIN game_price ON owned_game.game_id = game_price.game_id AND game_price.price_end_date is NULL
			GROUP BY title, price
			ORDER BY minutes_played ${req.query.order}`
				}
			} else {
				getAllGamesQuery = 'SELECT game.id, game.title, game_price.price FROM game INNER JOIN game_price ON game.id = game_price.id AND game_price.price_end_date IS NULL ORDER BY title ASC';
			}
		}
	}
	try {
		const { rows } = await dbQuery.query(getAllGamesQuery);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'There are no games';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured';
		return res.status(status.error).send(errorMessage);
	}
}

const getUserGames = async (req, res) => {
	var getUserGamesQuery
	if (req.query.sort === undefined) {
		getUserGamesQuery = 'SELECT game.title, owned_game.purchase_date FROM owned_game INNER JOIN game ON game_id = game.id WHERE client_id = $1 ORDER BY game.title ASC';
	} else {
		if (req.query.order === undefined) {
			getUserGamesQuery = `SELECT game.title, owned_game.purchase_date FROM owned_game INNER JOIN game ON game_id = game.id WHERE client_id = $1 ORDER BY ${req.query.sort} ASC`;
		} else {
			getUserGamesQuery = `SELECT game.title, owned_game.purchase_date FROM owned_game INNER JOIN game ON game_id = game.id WHERE client_id = $1 ORDER BY ${req.query.sort} ${req.query.order}`;
		}
	}
	const values = [req.user.id];
	try {
		const { rows } = await dbQuery.query(getUserGamesQuery, values);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'This user has no games';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get users games';
		return res.status(status.error).send(errorMessage);
	}
}

const getGamesOfGenre = async (req, res) => {
	var getGamesOfGenreQuery
	if (req.query.sort === undefined) {
		getGamesOfGenreQuery = 'SELECT game.title, game_price.price FROM game_genre INNER JOIN game ON game_id = game.id INNER JOIN genre ON genre_id = genre.id LEFT JOIN game_price ON game_genre.game_id = game_price.game_id AND game_price.price_end_date IS NULL WHERE genre.name = $1 ORDER BY game.title ASC';
	} else {
		if (req.query.order === undefined) {
			getGamesOfGenreQuery = `SELECT game.title, game_price.price FROM game_genre INNER JOIN game ON game_id = game.id INNER JOIN genre ON genre_id = genre.id LEFT JOIN game_price ON game_genre.game_id = game_price.game_id AND game_price.price_end_date IS NULL WHERE genre.name = $1 ORDER BY ${req.query.sort} ASC`;
		} else {
			getGamesOfGenreQuery = `SELECT game.title, game_price.price FROM game_genre INNER JOIN game ON game_id = game.id INNER JOIN genre ON genre_id = genre.id LEFT JOIN game_price ON game_genre.game_id = game_price.game_id AND game_price.price_end_date IS NULL WHERE genre.name = $1 ORDER BY ${req.query.sort} ${req.query.order}`;
		}
	}
	const values = [req.params.genre];
	if (values[0] === undefined) {
		errorMessage.error = 'Genre is not specified';
		return res.status(status.error).send(errorMessage);
	}
	try {
		const { rows } = await dbQuery.query(getGamesOfGenreQuery, values);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'There are no games in this genre';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get users games';
		return res.status(status.error).send(errorMessage);
	}
}

const getGenres = async (req, res) => {
	const getGenresQuery = 'SELECT name FROM genre';
	try {
		const { rows } = await dbQuery.query(getGenresQuery);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'There are no genres';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get genres';
		return res.status(status.error).send(errorMessage);
	}
}

const addUserGame = async (req, res) => {
	if (req.body.game_title === undefined) {
		errorMessage.error = 'Title of the game is not specified';
		return res.status(status.error).send(errorMessage);
	}
	if (req.body.purchase_date === undefined) {
		errorMessage.error = 'Purchase date is not specified';
		return res.status(status.error).send(errorMessage);
	}
	if (!validator.isISO8601(req.body.purchase_date)) {
		errorMessage.error = 'Date is in wrong format, use ISO8601';
		return res.status(status.error).send(errorMessage);
	}
	const game_title = req.body.game_title;
	const purchase_date = req.body.purchase_date;
	const addUserGameQuery = 'INSERT INTO owned_game(client_id, game_id, purchase_date) VALUES ($1, (SELECT id FROM game WHERE title = $2), $3)';
	const getUsersGames = 'SELECT game.title FROM owned_game INNER JOIN game ON game_id = game.id WHERE client_id = $1';
	var values = [req.user.id]
	var ownedGames = []
	try {
		const { rows } = await dbQuery.query(getUsersGames, values);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {

		} else {
			for (var i = 0; i < dbResponse.length; i++) {
				ownedGames.push(dbResponse[i].title)
			}
		}
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get users games';
		return res.status(status.error).send(errorMessage);
	}
	if (ownedGames.includes(game_title)) {
		errorMessage.error = 'User already has this game';
		return res.status(status.error).send(errorMessage);
	}
	values = [req.user.id, game_title, purchase_date]
	try {
		const { rows } = await dbQuery.query(addUserGameQuery, values);
		const dbResponse = rows[0];
		successMessage.data = dbResponse;
		return res.status(status.created).send(successMessage);
	} catch (error) {
		console.log(`${error}`);
		errorMessage.error = 'An error Occured while trying to add users game';
		return res.status(status.error).send(errorMessage);
	}
}


export {
	getAllGames,
	getUserGames,
	getGamesOfGenre,
	addUserGame,
	getGenres,
};