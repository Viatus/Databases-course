import express from 'express';

import { createUser, signinUser, updateUser, } from '../controllers/usersController';
import verifyAuth from '../middleware/verifyAuth';


const router = express.Router();

router.post('/auth/signup', createUser);
router.post('/auth/signin', signinUser);
router.post('/users/me', verifyAuth, updateUser);

export default router;
