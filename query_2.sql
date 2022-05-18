--Версия 1 - считаются лишь машины использованные в каждый день
WITH usage_per_dow (day_of_month, day_of_week, machine_count, second_sum) AS (
	SELECT date_part('day', in_use_from) as day_of_month, date_part('dow', in_use_from) as day_of_week, COUNT(DISTINCT machine.id), SUM(EXTRACT(EPOCH FROM in_use_to) - EXTRACT(EPOCH from in_use_from)) from machine_usage
	INNER JOIN machine ON machine_usage.machine_id = machine.id 
	WHERE (in_use_from >= current_date - interval '1 month' AND in_use_to <= current_date)
	GROUP BY day_of_month, day_of_week
)
SELECT day_of_week, AVG(ROUND((second_sum / 3600 / machine_count) :: numeric, 2))::integer as hours_occupied FROM usage_per_dow
GROUP BY day_of_week
ORDER BY day_of_week

--Версия два - машина считается даже если она в тот день не была использована
/*WITH usage_per_dow (day_of_month, day_of_week, second_sum) AS (
	SELECT date_part('day', in_use_from) as day_of_month, date_part('dow', in_use_from) as day_of_week, SUM(EXTRACT(EPOCH FROM in_use_to) - EXTRACT(EPOCH from in_use_from)) from machine_usage
	INNER JOIN machine ON machine_usage.machine_id = machine.id 
	WHERE (in_use_from >= current_date - interval '1 month' AND in_use_to <= current_date)
	GROUP BY day_of_month, day_of_week
),
machine_count (amount_of_machines) AS (
	SELECT COUNT(id) FROM machine
)
SELECT day_of_week, ROUND(AVG((second_sum / 3600 / amount_of_machines))::numeric, 10) as hours_occupied
FROM usage_per_dow, machine_count
GROUP BY day_of_week
ORDER BY day_of_week*/