import dbQuery from '../db/dev/dbQuery';

import {
	hashPassword,
	generateUserToken,
	comparePassword,
} from '../helpers/validation';

import {
  errorMessage, successMessage, status,
} from '../helpers/status';

var validator = require('validator');

const createUser = async (req, res) => {
	if (req.body.email === undefined || req.body.password === undefined || req.body.nickname === undefined) {
		errorMessage.error = 'Email, password or nickname field are not provided';
		return res.status(status.bad).send(errorMessage);
	}
	
	const {
		email, password, nickname,
	} = req.body;
	
		
	if (validator.isEmpty(email) || validator.isEmpty(password) || validator.isEmpty(nickname)) {
		errorMessage.error = 'Email, password and nickname field cannot be empty';
		return res.status(status.bad).send(errorMessage);
	}
	
	if (!validator.isEmail(email)) {
		errorMessage.error = 'Email is not valid';
		return res.status(status.bad).send(errorMessage);
	}
	
	if (!validator.isLength(password, 5)) {
		errorMessage.error = 'Password must be not shorter than 5 characters';
		return res.status(status.bad).send(errorMessage);
	}
	
	const hashedPassword = hashPassword(password);
	const createUserQuery = `INSERT INTO
      client(email, nickname, hash)
      VALUES($1, $2, $3)
      returning *`;
	const values = [
		email,
		nickname,
		hashedPassword
	];
	
	try {
		const { rows } = await dbQuery.query(createUserQuery, values);
		const dbResponse = rows[0];
		delete dbResponse.hash;
		const token = generateUserToken(dbResponse.id, dbResponse.email, dbResponse.nickname);
		successMessage.data = dbResponse;
		successMessage.data.token = token;
		return res.status(status.created).send(successMessage);
	} catch (error) {
		if (error.routine === '_bt_check_unique') {
			errorMessage.error = 'User with that EMAIL already exist';
			return res.status(status.conflict).send(errorMessage);
		}
		errorMessage.error = 'Error occured while trying to create user';
		return res.status(status.error).send(errorMessage);
	}
}

const signinUser = async (req, res) => {
	const { email, password } = req.body;
	
	if (validator.isEmpty(email) || validator.isEmpty(password)) {
		errorMessage.error = 'Email or Password detail is missing';
		return res.status(status.bad).send(errorMessage);
	}
	
	if (!validator.isEmail(email) || !validator.isLength(password, 5)) {
		errorMessage.error = 'Please enter a valid Email or Password';
		return res.status(status.bad).send(errorMessage);
	}

	const signinUserQuery = 'SELECT * FROM client WHERE email = $1';
	try {
		const { rows } = await dbQuery.query(signinUserQuery, [email]);
		const dbResponse = rows[0];
		
		if (!dbResponse) {
			errorMessage.error = 'User with this email does not exist';
			return res.status(status.notfound).send(errorMessage);
		}
		
		if (comparePassword(password, dbResponse.hash)) {
			errorMessage.error = 'The password you provided is incorrect';
			return res.status(status.bad).send(errorMessage);
		}
		const token = generateUserToken(dbResponse.id, dbResponse.email, dbResponse.nickname);
		delete dbResponse.hash;
		successMessage.data = dbResponse;
		successMessage.data.token = token;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'Error occured while trying to signin';
		return res.status(status.error).send(errorMessage);
	}
}

const updateUser = async (req, res) => {
	if (req.body.password === undefined && req.body.nickname === undefined) {
		errorMessage.error = 'Nothing to update';
		return res.status(status.bad).send(errorMessage);
	}
	
	var updateUserQuery;
	var values;
	if (req.body.password !== undefined && req.body.nickname !== undefined) {
		if (validator.isEmpty(req.body.password) || validator.isEmpty(req.body.nickname)) {
			errorMessage.error = 'Password and nickname field cannot be empty';
			return res.status(status.bad).send(errorMessage);
		}
		if (!validator.isLength(req.body.password, 5)) {
			errorMessage.error = 'Password must be not shorter than five(5) characters';
			return res.status(status.bad).send(errorMessage);
		}
		const hashedPassword = hashPassword(req.body.password);
		updateUserQuery = `UPDATE client SET nickname = $1, hash = $2 WHERE id = $3 returning *`;
		values = [req.body.nickname, hashedPassword, req.user.id];
	} else {
		if (req.body.password !== undefined) {
			if (validator.isEmpty(req.body.password)) {
				errorMessage.error = 'Password field cannot be empty';
				return res.status(status.bad).send(errorMessage);
			}
			if (!validator.isLength(req.body.password, 5)) {
				errorMessage.error = 'Password must be not shorter than five(5) characters';
				return res.status(status.bad).send(errorMessage);
			}
			const hashedPassword = hashPassword(req.body.password);
			updateUserQuery = `UPDATE client SET hash = $1 WHERE id = $2 returning *`;
			values = [hashedPassword, req.user.id];
		} else {
			if (validator.isEmpty(req.body.nickname)) {
				errorMessage.error = 'Nickname field cannot be empty';
				return res.status(status.bad).send(errorMessage);
			}
			values = [req.body.nickname, req.user.id];
			updateUserQuery = `UPDATE client SET nickname = $1 WHERE id = $2 returning *`;
		}
	}

	try {
		const { rows } = await dbQuery.query(updateUserQuery, values);
		const dbResponse = rows[0];
		delete dbResponse.hash;
		const token = generateUserToken(dbResponse.id, dbResponse.email, dbResponse.nickname);
		successMessage.data = dbResponse;
		successMessage.data.token = token;
		return res.status(status.created).send(successMessage);
	} catch (error) {
		errorMessage.error = 'Error occured while trying to alter user';
		return res.status(status.error).send(errorMessage);
	}
}

export {
	createUser,
	signinUser,
	updateUser,
};