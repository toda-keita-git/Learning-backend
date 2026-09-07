-- 「1日時間割」機能用のテーブル定義。
-- このプロジェクトはマイグレーションツールを使っていないため、
-- 本番/開発DBへは手動で1回だけ実行する想定の参照用スクリプト（plan_schema.sql等と同じ運用）。
--
-- 1日単位の予定を分単位で保持する。日付・時刻は常に利用者のローカル日付・
-- ローカル時刻として扱い、timezoneはあくまで記録（表示・将来の海外利用対応）用で、
-- サーバー側の判定には使わない（JSTのみを前提にしている既存の日付処理と揃える）。
--
-- source_type/source_idで既存のプラン・メモ（習慣もメモの一種）に紐づけられる。
-- source_type = 'custom' の場合はsource_idを持たない、時間割単独の予定。
-- 紐づけ先が削除された場合に備え、作成時点のタイトルをtitle_snapshotへ複製して保持する
-- （プラン・メモのタイトル変更やソフト削除後も、時間割上の履歴は残したいため）。

CREATE TABLE IF NOT EXISTS daily_schedule_items (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    schedule_date DATE NOT NULL,
    start_minute SMALLINT NOT NULL,
    end_minute SMALLINT NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Tokyo',
    source_type VARCHAR(16) NOT NULL, -- 'plan' / 'note' / 'habit' / 'custom'
    source_id INTEGER, -- source_type='custom'ならNULL。それ以外はplans.id または notes.id
    title_snapshot VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'planned', -- 'planned' / 'done' / 'skipped'
    color_key VARCHAR(24),
    delete_flg INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_daily_schedule_minutes CHECK (
        start_minute >= 0 AND start_minute < 1440
        AND end_minute > start_minute AND end_minute <= 1440
    ),
    CONSTRAINT chk_daily_schedule_source_type CHECK (
        source_type IN ('plan', 'note', 'habit', 'custom')
    ),
    CONSTRAINT chk_daily_schedule_status CHECK (
        status IN ('planned', 'done', 'skipped')
    )
);

-- 「今日の時間割を取得」で必ず使う絞り込み（user_id + schedule_date）
CREATE INDEX IF NOT EXISTS idx_daily_schedule_user_date
    ON daily_schedule_items (user_id, schedule_date)
    WHERE delete_flg = 0;
