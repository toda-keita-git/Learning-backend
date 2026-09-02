-- 既存の本番・開発DBへ、開始日と期限日を安全に追加する差分SQL。
-- 既存レコードはどちらもNULLになり、現在の表示・進捗計算には影響しない。

ALTER TABLE plans ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE plans ADD COLUMN IF NOT EXISTS due_date DATE;

CREATE INDEX IF NOT EXISTS idx_plans_user_due_date
    ON plans(user_id, due_date)
    WHERE delete_flg = 0;

COMMENT ON COLUMN plans.start_date IS '任意の取り組み開始日';
COMMENT ON COLUMN plans.due_date IS '任意の目標・アクションプラン期限日';
