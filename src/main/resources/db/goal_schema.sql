-- 目標達成支援アプリ用のテーブル定義（仕様書「DB設計」章に対応）
-- このプロジェクトはマイグレーションツールを使っていないため、
-- 本番/開発DBへは手動で1回だけ実行する想定の参照用スクリプト。
-- categories / tags は既存テーブルをそのまま流用する。

CREATE TABLE IF NOT EXISTS goals (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress', -- in_progress / achieved / suspended
    delete_flg INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS action_plans (
    id SERIAL PRIMARY KEY,
    goal_id INTEGER NOT NULL REFERENCES goals(id),
    user_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0, -- 表示順。小さいほど上（D&Dで書き換え）
    status VARCHAR(20) NOT NULL DEFAULT 'not_started', -- not_started / in_progress / done
    delete_flg INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    action_plan_id INTEGER REFERENCES action_plans(id), -- NULL = 未紐付け
    type VARCHAR(10) NOT NULL, -- 'learning' / 'task' / 'normal'
    title VARCHAR(255) NOT NULL,
    body TEXT,
    mastery INTEGER,   -- type='learning' の場合のみ 0-100
    progress INTEGER,  -- type='task' の場合のみ 0-100（todo未使用時の手入力値）
    category_id INTEGER REFERENCES categories(id),
    github_path TEXT,
    commit_sha VARCHAR(64),
    repo_name VARCHAR(255),
    delete_flg INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS note_todo_items (
    id SERIAL PRIMARY KEY,
    note_id INTEGER NOT NULL REFERENCES notes(id),
    label VARCHAR(255) NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS note_tags (
    id SERIAL PRIMARY KEY,
    note_id INTEGER NOT NULL REFERENCES notes(id),
    tag_id INTEGER NOT NULL REFERENCES tags(id)
);

CREATE INDEX IF NOT EXISTS idx_action_plans_goal_id ON action_plans(goal_id);
CREATE INDEX IF NOT EXISTS idx_notes_action_plan_id ON notes(action_plan_id);
CREATE INDEX IF NOT EXISTS idx_note_todo_items_note_id ON note_todo_items(note_id);
CREATE INDEX IF NOT EXISTS idx_note_tags_note_id ON note_tags(note_id);

-- 移行（仕様書「移行計画」章）：既存の learnings を学習用メモとして未紐付けで移す。
-- learnings / learning_tags は削除せず残し、検証後に別途廃止する。
INSERT INTO notes (user_id, action_plan_id, type, title, body, mastery, category_id, github_path, commit_sha, repo_name, created_at, delete_flg)
SELECT user_id, NULL, 'learning', title, explanatory_text, understanding_level, category_id, github_path, commit_sha, repo_name, created_at, delete_flg
FROM learnings;

-- 注意: 上記の学習用メモへの移行はSERIALで新しいidを振り直すため、
-- 旧learning_tagsの行を新しいnote_tagsへ機械的に対応付けることはできない
-- （learning.id → note.id の対応表が無いため）。タグ付けは移行後、
-- 手動で付け直すか、対応表を別途作ってから移行する。
