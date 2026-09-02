-- 目標達成支援アプリ v0.3 用のテーブル定義・移行スクリプト（仕様書「DB設計」「移行計画」章に対応）
-- このプロジェクトはマイグレーションツールを使っていないため、
-- 本番/開発DBへは手動で1回だけ実行する想定の参照用スクリプト。
--
-- 前提: 既に db/goal_schema.sql（v0.2: goals / action_plans / notes.action_plan_id 等）を
-- 適用済みであること。まだの場合は、手順2〜5（移行部分）は不要なので飛ばし、
-- 手順1（テーブル作成）だけを実行してから notes テーブルを作成する。

-- ============================================================
-- 1. 新テーブル作成
-- ============================================================

CREATE TABLE IF NOT EXISTS plans (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    parent_id INTEGER REFERENCES plans(id), -- NULL = ルート（目標として表示）
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'not_started', -- not_started / in_progress / done / suspended
    start_date DATE,
    due_date DATE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    delete_flg INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_plans_parent_id ON plans(parent_id);
CREATE INDEX IF NOT EXISTS idx_plans_user_due_date ON plans(user_id, due_date) WHERE delete_flg = 0;

CREATE TABLE IF NOT EXISTS note_plan_links (
    id SERIAL PRIMARY KEY,
    note_id INTEGER NOT NULL REFERENCES notes(id),
    plan_id INTEGER NOT NULL REFERENCES plans(id),
    UNIQUE (note_id, plan_id)
);
CREATE INDEX IF NOT EXISTS idx_note_plan_links_plan_id ON note_plan_links(plan_id);
CREATE INDEX IF NOT EXISTS idx_note_plan_links_note_id ON note_plan_links(note_id);

CREATE TABLE IF NOT EXISTS note_attachments (
    id SERIAL PRIMARY KEY,
    note_id INTEGER NOT NULL REFERENCES notes(id),
    kind VARCHAR(10) NOT NULL, -- 'image' / 'code'
    github_path TEXT NOT NULL,
    commit_sha VARCHAR(64),
    repo_name VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_note_attachments_note_id ON note_attachments(note_id);

-- ============================================================
-- 2. goals / action_plans を plans へ統合
--    goalのidはそのまま引き継ぎ、action_planのidは goals.id との衝突を避けるため
--    +1,000,000 のオフセットで採番し直す（goalが百万件規模になることは想定しない）
-- ============================================================

INSERT INTO plans (id, user_id, parent_id, title, description, status, sort_order, delete_flg, created_at)
SELECT id, user_id, NULL, title, description, status, 0, delete_flg, created_at
FROM goals;

INSERT INTO plans (id, user_id, parent_id, title, description, status, sort_order, delete_flg, created_at)
SELECT id + 1000000, user_id, goal_id, title, NULL, status, priority, delete_flg, created_at
FROM action_plans;

-- 明示的にidを指定して挿入したため、plansのSERIALシーケンスは追従していない。
-- 以降にAPI経由で作成されるプランが既存idと衝突しないよう、続きの番号から採番されるように調整する
SELECT setval('plans_id_seq', COALESCE((SELECT MAX(id) FROM plans), 1));

-- ============================================================
-- 3. notes.action_plan_id（単一リンク）を note_plan_links（多対多）へ変換
--    旧action_plan_idは、手順2のオフセットを加えたplans.idに読み替える
-- ============================================================

INSERT INTO note_plan_links (note_id, plan_id)
SELECT id, action_plan_id + 1000000
FROM notes
WHERE action_plan_id IS NOT NULL;

-- ============================================================
-- 4. notes.github_path 等（単一のコード添付）を note_attachments へ変換
-- ============================================================

INSERT INTO note_attachments (note_id, kind, github_path, commit_sha, repo_name, sort_order)
SELECT id, 'code', github_path, commit_sha, repo_name, 0
FROM notes
WHERE github_path IS NOT NULL AND github_path <> '';

-- ============================================================
-- 5. 検証後、notesから旧カラムを削除する
--    （アプリのリリース直後は残しておき、動作確認できてから実行するのが安全）
-- ============================================================

ALTER TABLE notes DROP COLUMN IF EXISTS action_plan_id;
ALTER TABLE notes DROP COLUMN IF EXISTS github_path;
ALTER TABLE notes DROP COLUMN IF EXISTS commit_sha;
ALTER TABLE notes DROP COLUMN IF EXISTS repo_name;

-- ============================================================
-- 6. goals / action_plans は、learnings / learning_tags と同様に
--    しばらく残しておき、検証後にあらためて廃止を判断する（このスクリプトでは削除しない）
-- ============================================================
