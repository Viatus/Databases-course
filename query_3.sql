WITH game_played_time(title, minutes_played, month_played) AS (
	SELECT game.title, SUM((date_part('hour', machine_usage.in_use_to) * 60 + date_part('minute', machine_usage.in_use_to)
					   - date_part('hour', machine_usage.in_use_from) * 60 - date_part('minute', machine_usage.in_use_from))) as minutes_played,
						date_part('month',machine_usage.in_use_to) + date_part('year',machine_usage.in_use_to) * 12 as month_from_beginning_played
	FROM owned_game
	RIGHT JOIN machine_usage ON owned_game.id = machine_usage.owned_game_id
	INNER JOIN game ON owned_game.game_id = game.id
	GROUP BY game.title, month_from_beginning_played
	ORDER BY game.title, month_from_beginning_played
),
cte2 AS (SELECT title, minutes_played, month_played, LAG(minutes_played, 1) OVER (PARTITION BY title ORDER BY month_played) as previous_month_playtime,
		 LAG(minutes_played, 2) OVER (PARTITION BY title ORDER BY month_played) as pre_previous_month_playtime
FROM game_played_time)
SELECT DISTINCT title FROM cte2
WHERE previous_month_playtime > pre_previous_month_playtime AND minutes_played > previous_month_playtime