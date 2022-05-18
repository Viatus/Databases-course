import dbQuery from '../db/dev/dbQuery';

import {
  errorMessage, successMessage, status,
} from '../helpers/status';
import validator from 'validator';

const getUserMachineUsage = async (req, res) => {
	const getUserMachineUsageQuery = 'SELECT game.title, in_use_from, in_use_to, machine_id FROM machine_usage LEFT JOIN owned_game ON owned_game_id = owned_game.id LEFT JOIN game ON owned_game.game_id = game.id WHERE owned_game.client_id = $1 ORDER BY in_use_from DESC';
    const values = [req.user.id];
    try {
		const { rows } = await dbQuery.query(getUserMachineUsageQuery, values);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'There is no machine usage';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get machine usage';
		return res.status(status.error).send(errorMessage);
	}
};

const createUserMachineUsage = async (req, res) => {
    const createUserMachineUsageQuery = `INSERT INTO machine_usage (owned_game_id, machine_id, in_use_from, in_use_to) VALUES ((SELECT id from owned_game WHERE client_id = $1 AND game_id = (SELECT id FROM game WHERE title = $2)), $3, $4, $5) returning *`;
    const usage_time = req.body.usage_time;
    if (usage_time === undefined) {
        errorMessage.error = 'Dates are missing';
		return res.status(status.error).send(errorMessage);
    }
    const dates = usage_time.split(";");
    if (dates[1] === undefined) {
        errorMessage.error = 'You need to specify both dates';
		return res.status(status.error).send(errorMessage);
    }
    if (!validator.isISO8601(dates[0]) || !validator.isISO8601(dates[1])) {
        errorMessage.error = 'Date format is wrong, use ISO8601';
		return res.status(status.error).send(errorMessage);
    }
    const game_title = req.body.game_title;
    if (game_title === undefined) {
        errorMessage.error = 'You need to specify game title';
		return res.status(status.error).send(errorMessage);
    }

    const checkIfUserHasGameQuery = 'SELECT * FROM owned_game WHERE client_id = $1 AND game_id = (SELECT id FROM game WHERE title = $2)';
    try {
        const { rows } = await dbQuery.query(checkIfUserHasGameQuery, [`${req.user.id}`, `${game_title}`]);
        const dbResponse = rows;
        if (dbResponse[0] === undefined) {
            errorMessage.error = 'User doesnt have this game';
            return res.status(status.bad).send(errorMessage);
        }
    } catch (error) {
        console.log(`${error}`);
        errorMessage.error = 'An error Occured while tring to check if user has game';
		return res.status(status.error).send(errorMessage);
    }

    const getFreeMachinesQuery = 'SELECT id FROM machine WHERE id NOT IN (SELECT machine_id FROM machine_usage WHERE in_use_from > $1 OR in_use_to < $2 GROUP BY machine_id)';
    var machineId = -1;
    try {
        const { rows } = await dbQuery.query(getFreeMachinesQuery, [`\'${dates[1]}\'`, `\'${dates[0]}\'`]);
        const dbResponse = rows;
        if (dbResponse[0] === undefined) {
            errorMessage.error = 'There is no free machines';
            return res.status(status.bad).send(errorMessage);
        }
        machineId = dbResponse[0].id;
    } catch (error) {
        errorMessage.error = 'An error Occured while tring to get free machines';
		return res.status(status.error).send(errorMessage);
    }

    const values = [req.user.id, game_title, machineId, dates[0], dates[1]];
    try {
		const { rows } = await dbQuery.query(createUserMachineUsageQuery, values);
		const dbResponse = rows[0];
		successMessage.data = dbResponse;
		return res.status(status.created).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to add machine usage';
		return res.status(status.error).send(errorMessage);
	}
};

export {
    getUserMachineUsage,
    createUserMachineUsage,
};