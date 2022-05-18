import env from '../../env';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const saltRounds = 10;
const salt = bcrypt.genSaltSync(saltRounds);
const hashPassword = password => bcrypt.hashSync(password, salt);


const generateUserToken = (id, email, nickname) => {
	const token = jwt.sign({
		id,
		email,
		nickname
	},
	env.secret, {expiresIn: '3d'});
	return token
}

const comparePassword = (hashedPassword, password) => {
  return bcrypt.compareSync(password, hashedPassword);
};

export {
  hashPassword,
  generateUserToken,
  comparePassword,
};
