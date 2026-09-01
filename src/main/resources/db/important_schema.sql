-- メモに「重要」フラグを追加するための移行スクリプト。
-- plan_schema.sql（v0.3）適用済みのDBに対して1回だけ実行する。
--
-- important: trueのメモは「今日やること・次にやる事」画面の一番下に、
-- 目標との紐づけに関わらず横断的に表示される。

ALTER TABLE notes ADD COLUMN IF NOT EXISTS important BOOLEAN NOT NULL DEFAULT false;
