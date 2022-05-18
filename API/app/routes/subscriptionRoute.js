import express from 'express';

import {getAllSubPlans, getUserSubscriptionPlans, addUserSubscriptionPlan} from '../controllers/subscriptionController';
import verifyAuth from '../middleware/verifyAuth';

const router = express.Router();

router.get('/subscription_plans', getAllSubPlans);
router.get('/subscription_plans/my', verifyAuth, getUserSubscriptionPlans);
router.post('/subscription_plans/my', verifyAuth, addUserSubscriptionPlan);

export default router;