-- Googleドライブ連携（GitHub連携と並存）のためのカラム追加。
-- このプロジェクトはマイグレーションツールを使っていないため、
-- 本番/開発DBへは手動で1回だけ実行する想定の参照用スクリプト（plan_schema.sql等と同じ運用）。
--
-- 既存カラム・既存データは一切変更しない、追加のみ。GitHubログイン済みユーザーの
-- 行では新カラムはすべてNULL（auth_providerのみデフォルト値'github'）のままになり、
-- 既存のGitHub連携の動作には影響しない。

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(10) NOT NULL DEFAULT 'github';
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_refresh_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS drive_folder_id VARCHAR(255);

-- google_subはGoogleアカウントごとに一意（NULLは複数許容＝GitHubユーザーは対象外）
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_sub ON users(google_sub) WHERE google_sub IS NOT NULL;

-- 添付がGitHubリポジトリ由来かGoogleドライブ由来かを区別する。
-- Googleドライブの添付は github_path カラムにDriveのfileIdを、
-- repo_name カラムにdrive_folder_idを流用して保存する
-- （カラム名はGitHub由来のままだが、リネームによる影響範囲拡大を避けるための意図的な流用）。
ALTER TABLE note_attachments ADD COLUMN IF NOT EXISTS provider VARCHAR(10) NOT NULL DEFAULT 'github';
