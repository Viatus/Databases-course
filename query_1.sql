WITH money_spent_on_sub(email, money_spent) AS (
	SELECT email, SUM(((date_part('day', active_to::timestamp - active_from::timestamp) / 31) + 1)::integer * price) as money_spent
	FROM (SELECT client.email, subscription_plan.price, client_subscription_plan.active_from, client_subscription_plan.active_to
		  FROM client_subscription_plan
		  INNER JOIN client ON client_subscription_plan.client_id = client.id
	  	  INNER JOIN subscription_plan ON client_subscription_plan.subscription_plan_id = subscription_plan.id) AS sub_plans
		  GROUP BY email),
money_spent_on_games (email, money_spent) AS (
	SELECT email, SUM(price) as money_spent
	FROM (SELECT client.email, game_price.price
		  FROM owned_game
		  INNER JOIN client ON owned_game.client_id = client.id
		  INNER JOIN game_price ON owned_game.game_id = game_price.game_id AND ((owned_game.purchase_date BETWEEN game_price.price_set_date AND game_price.price_end_date) 
																				OR (owned_game.purchase_date >= game_price.price_set_date AND game_price.price_end_date is NULL))) AS game_prices
		  GROUP BY email)
SELECT COALESCE(money_spent_on_sub.email, money_spent_on_games.email) as client_email, COALESCE(money_spent_on_sub.money_spent, 0) + COALESCE(money_spent_on_games.money_spent, 0) as total_money_spent
FROM money_spent_on_sub
FULL JOIN money_spent_on_games ON money_spent_on_sub.email = money_spent_on_games.email