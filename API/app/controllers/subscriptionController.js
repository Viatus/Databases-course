import dbQuery from '../db/dev/dbQuery';

import {
  errorMessage, successMessage, status,
} from '../helpers/status';
import validator from 'validator';
import format from 'date-format';


const getAllSubPlans = async (req, res) => {
	const getAllSubPlansQuery = 'SELECT * FROM subscription_plan ORDER BY price';
	try {
		const { rows } = await dbQuery.query(getAllSubPlansQuery);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'There are no subscription plans';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get suscription plans';
		return res.status(status.error).send(errorMessage);
	}
};

const getUserSubscriptionPlans = async(req, res) => {
	const getUserSubscriptionPlansQuery = 'SELECT subscription_plan.name, active_from, active_to FROM client_subscription_plan INNER JOIN subscription_plan ON subscription_plan_id = subscription_plan.id WHERE client_id = $1';
	const values = [req.user.id]
	try {
		const { rows } = await dbQuery.query(getUserSubscriptionPlansQuery, values);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
			errorMessage.error = 'This user has no subscription plans';
			return res.status(status.notfound).send(errorMessage);
		}
		successMessage.data = dbResponse;
		return res.status(status.success).send(successMessage);
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get user suscription plans';
		return res.status(status.error).send(errorMessage);
	}
};

const addUserSubscriptionPlan = async(req, res) => {
	const addUserSubscriptionPlanQuery = 'INSERT INTO client_subscription_plan (client_id, subscription_plan_id, active_from, active_to) VALUES ($1, (SELECT id FROM subscription_plan WHERE name = $2), $3, $4)';
	const {
		plan_name, active_from, active_to
	} = req.body;
	if (plan_name === undefined || active_from === undefined || active_to === undefined) {
		errorMessage.error = 'plan_name, active_from or active_to is missing';
		return res.status(status.error).send(errorMessage);
	}

	if(!validator.isISO8601(active_from) || !validator.isISO8601(active_to)) {
		errorMessage.error = 'Date format is wrong, use ISO8601';
		return res.status(status.error).send(errorMessage);
	}

	const userPlanPeriodQuery = 'SELECT active_from, active_to FROM client_subscription_plan WHERE client_id = $1 AND subscription_plan_id = (SELECT id FROM subscription_plan WHERE name = $2)'
	var dates = [[]];
	try {
		const { rows } = await dbQuery.query(userPlanPeriodQuery, [req.user.id, plan_name]);
		const dbResponse = rows;
		if (dbResponse[0] === undefined) {
		} else {
			for (var i =0; i < dbResponse.length; i++) {
				dates.push([dbResponse[i].active_from, dbResponse[i].active_to]);
			}
		}
	} catch (error) {
		errorMessage.error = 'An error Occured while trying to get user suscription plans';
		return res.status(status.error).send(errorMessage);
	}

	for (var i =0; i < dates.length; i++) {
		if ((validator.isBefore(active_from, format.asString(dates[i][1])) && validator.isAfter(active_from, format.asString(dates[i][0]))) || (validator.isBefore(active_to, format.asString(dates[i][1])) && validator.isAfter(active_to, format.asString(dates[i][0])))) {
			errorMessage.error = 'User already has this plan for this period';
			return res.status(status.error).send(errorMessage);
		}
	}

	const values = [req.user.id, plan_name, active_from, active_to];
	try {
		const { rows } = await dbQuery.query(addUserSubscriptionPlanQuery, values);
		const dbResponse = rows[0];
		successMessage.data = dbResponse;
		return res.status(status.created).send(successMessage);
	} catch (error) {
        console.log(`${error}`);
		errorMessage.error = 'An error Occured while trying to add users subscription plan';
		return res.status(status.error).send(errorMessage);
	}
};


export {
	getAllSubPlans,
	getUserSubscriptionPlans,
	addUserSubscriptionPlan,
}