import express from 'express';

import {getAllGames, getUserGames, getGamesOfGenre, addUserGame, getGenres} from '../controllers/gamesController';
import verifyAuth from '../middleware/verifyAuth';

const router = express.Router();

router.get('/games', getAllGames);
router.get('/games/my', verifyAuth, getUserGames);
router.post('/games/my', verifyAuth, addUserGame);
router.get('/games/genre/:genre', getGamesOfGenre);
router.get('/genres', getGenres);

export default router;