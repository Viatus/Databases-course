import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';
import {
	errorMessage, status,
} from '../helpers/status';
import dbQuery from '../db/dev/dbQuery';


dotenv.config();

const verifyToken = async (req, res, next) => {
	const token = req.get('token');
	if (!token) {
		errorMessage.error = 'Token not provided';
		return res.status(status.bad).send(errorMessage);
	}
	try {
		const decoded = jwt.verify(token, process.env.SECRET);
		req.user = {
			email: decoded.email,
			id: decoded.id,
			nickname: decoded.nickname
		};
		const checkUserQuery = 'SELECT * FROM client WHERE id = $1 AND email = $2 AND nickname = $3';
		try {
			const { rows } = await dbQuery.query(checkUserQuery, [req.user.id, req.user.email, req.user.nickname]);
			const dbResponse = rows[0];

			if (!dbResponse) {
				errorMessage.error = 'Authentification failed';
				return res.status(status.notfound).send(errorMessage);
			}
		} catch (error) {
			console.log(`${error}`);
			errorMessage.error = 'Error occured while trying to verify token';
			return res.status(status.error).send(errorMessage);
		}
		next();
	} catch (error) {
		errorMessage.error = 'Authentication Failed';
		return res.status(status.unauthorized).send(errorMessage);
	}
};

export default verifyToken;