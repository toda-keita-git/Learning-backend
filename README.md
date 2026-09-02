# 目標達成支援アプリ（バックエンド）

目標とアクションプランを1つの再帰的な「プラン」に統合し、メモをプランから独立させて
多対多でリンクできるようにした構成の **バックエンド API** です（v0.3）。
Java / Spring Boot で実装し、プラン・メモ（学習用／タスク用／通常）の永続化、
進捗の再帰的な自動集計、GitHub OAuth 認証、タグ／カテゴリ管理を担います。

> 🔗 フロントエンド（React / TypeScript）：https://github.com/toda-keita-git/Learning-frontend
>
> 元々は「学習ログ」アプリとして開発していたコードを `old/` に残しています。詳細仕様は
> フロントエンド／バックエンド双方の会話履歴からまとめた仕様書（Artifact）を参照してください。

---

## 📌 担当している役割

- **プラン の CRUD API**（`PlanController`）：親を持たないプランは目標、親を持つプランはアクションプランとしてUI側で表示が切り替わるだけで、実体は同じ1テーブル
- **開始日・期限日の管理**：目標とアクションプランに任意の日付を設定し、開始日より前の期限はAPIで拒否
- **プランの再配置（親の変更）**：ドラッグ・タップどちらの操作からも呼べる `plan_reparent` で、循環参照にならないかサーバー側で検証してから更新
- **進捗の再帰的な自動集計**：直属メモの実効進捗と子プランの達成率をまとめて平均し、末端から根まで積み上げる（`ProgressService`）
- **メモ の CRUD API**（`NoteController`）：プランに従属しない独立したエンティティとして作成・編集・削除
- **メモ↔プランの多対多リンク**（`note_link` / `note_unlink`）と、**複数の画像・コード添付**（`note_attachment_insert` / `note_attachment_delete`）
- **タグ／カテゴリの管理**（既存資産を流用し、メモへのタグ付け`note_tags`も新設）
- **フリープランの登録上限**（メモ100件・カテゴリー20件・タグ50件。新規作成のみブロックし、既存の記録は削除しない）
- **GitHub OAuth**：フロントから受け取った認可コードをアクセストークンに交換し、ユーザーを DB に登録・更新
- **PostgreSQL への永続化**（MyBatis によるマッパー実装。テーブル定義は [`src/main/resources/db/plan_schema.sql`](./src/main/resources/db/plan_schema.sql)。v0.2時点の定義は [`db/goal_schema.sql`](./src/main/resources/db/goal_schema.sql) に残しています）

## 🛠 使用技術

| 分類 | 技術 |
|---|---|
| 言語 | Java 17 |
| フレームワーク | Spring Boot（Spring Web） |
| DB アクセス | MyBatis |
| データベース | PostgreSQL |
| ビルド | Maven |
| デプロイ | Docker（マルチステージビルド） |

## 🧩 主なエンドポイント

| メソッド | パス | 内容 |
|---|---|---|
| GET  | `/plans` | プラン一覧を取得（`parent_id`込み、達成率込み） |
| POST | `/plan_insert` / `/plan_update/{id}` / `/plan_delete/{id}` | プランの登録・更新・削除 |
| POST | `/plan_reparent/{id}` | 親の変更（＝再配置）。循環参照になる場合は400 |
| POST | `/plan_reorder` | 同じ親を持つプラン同士の並べ替え一括更新 |
| GET  | `/notes` | メモ一覧を取得（学習用／タスク用／通常。`links`・`attachments`込み） |
| POST | `/note_insert` / `/note_update/{id}` / `/note_delete/{id}` | メモの登録・更新・削除 |
| POST | `/note_link/{id}` / `/note_unlink/{id}` | メモとプランのリンクを追加・削除 |
| POST | `/note_todo_toggle/{id}` | タスク用メモのtodoチェック切替 |
| POST | `/note_attachment_insert/{id}` / `/note_attachment_delete/{attachmentId}` | 画像・コード添付の追加・削除 |
| GET  | `/category_list` / `/tag_list` | カテゴリ・タグ一覧 |
| POST | `/github/callback` | GitHub OAuth コールバック（コード→トークン交換＋ユーザー登録） |

## 🏗 構成

```
src/main/java/com/udemy/hello/
├── LearningApplication.java     … エントリポイント
├── PlanController.java          … プランの REST コントローラ
├── NoteController.java          … メモの REST コントローラ
├── LearningController.java      … カテゴリ／タグ・GitHub連携先切替・お問い合わせ等の共通API
├── GitHubAuthController.java    … GitHub OAuth コールバック
├── mapper/                      … Service / MyBatis Mapper（ProgressServiceが再帰的な進捗集計を担当）
└── model/                       … エンティティ（Plan, Note, NoteAttachment, NotePlanLink, NoteTodoItem 等）
src/main/resources/
├── application.properties       … 設定（秘密情報は環境変数から注入）
├── db/plan_schema.sql           … v0.3用テーブルのDDL・goals/action_plans統合の移行スクリプト
├── db/plan_deadline_schema.sql  … 既存DBへ開始日・期限日を追加する差分SQL
├── db/goal_schema.sql           … v0.2時点のテーブル定義（参照用に保持）
└── mapper/*.xml                 … MyBatis の SQL 定義

old/                              … 元の「学習ログ」アプリ時代のソース一式（参照用に保持）
```

## 🚀 起動方法

秘密情報はソースに含めず、**環境変数**から読み込みます。必要な変数は
[`application.properties.example`](./src/main/resources/application.properties.example) を参照してください。

```bash
# 環境変数を設定（DB_URL / DB_USERNAME / DB_PASSWORD / GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET）
./mvnw spring-boot:run
```

Docker で動かす場合：

```bash
docker build -t learning-backend .
docker run -p 8080:8080 --env-file .env learning-backend
```

既存のデータベースへ期限機能を追加する場合は、デプロイ前に次の差分SQLを実行してください。

```bash
psql "$DB_URL" -f src/main/resources/db/plan_deadline_schema.sql
```

## 💡 制作の背景

「学習メモ」と「GitHub 上のコード」を結びつけて残せる学習ログアプリを、
フロントエンドからバックエンドまで一貫して自作しました。
本バックエンドでは、REST API 設計・DB 設計（多対多のタグ管理）・OAuth のトークン交換など、
実務でも用いられる構成を Java / Spring Boot で実装しています。
