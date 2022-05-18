import express from 'express';

import {getUserMachineUsage, createUserMachineUsage} from '../controllers/machineUsageController';
import verifyAuth from '../middleware/verifyAuth';

const router = express.Router();

router.post('/sessions/my', verifyAuth, createUserMachineUsage);
router.get('/sessions/my', verifyAuth, getUserMachineUsage);

export default router;