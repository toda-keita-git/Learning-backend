-- 「今日の復習」を、固定ペースの繰り返しやること（毎日/週1/月1、日数は自由設定）へ作り変えるための追加移行スクリプト。
-- plan_schema.sql（v0.3）適用済みのDBに対して1回だけ実行する。
--
-- review_interval_days: NULLなら繰り返し対象外。設定時はNULLの完了ごとにN日後を次回期日とする
-- （期日の判定・完了記録自体はローカルストレージ側で行うため、DBにはペースの設定値だけを持つ）

ALTER TABLE notes ADD COLUMN IF NOT EXISTS review_interval_days INTEGER;
