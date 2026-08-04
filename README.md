# 目標達成支援アプリ（バックエンド）

最終目標 → アクションプラン → メモ の構造で「やることの明確化」を支援するアプリの **バックエンド API** です。
Java / Spring Boot で実装し、目標・アクションプラン・メモ（学習用／タスク用／通常）の永続化、
進捗の自動集計、GitHub OAuth 認証、タグ／カテゴリ管理を担います。

> 🔗 フロントエンド（React / TypeScript）：https://github.com/toda-keita-git/Learning-frontend
>
> 元々は「学習ログ」アプリとして開発していたコードを `old/` に残しています。詳細仕様は
> フロントエンド／バックエンド双方の会話履歴からまとめた仕様書（Artifact）を参照してください。

---

## 📌 担当している役割

- **目標 / アクションプラン / メモ の CRUD API**（`GoalController` / `ActionPlanController` / `NoteController`）
- **進捗の自動集計**：メモの習熟度・進捗度（todoの消化率含む）から、アクションプラン→目標の達成率を都度算出（`ProgressService`）
- **アクションプランの優先順位付け**（ドラッグ&ドロップ確定後の一括並べ替えAPI）
- **メモの後からの紐付け**（未紐付けメモをアクションプランへ後から紐付け）
- **タグ／カテゴリの管理**（既存資産をそのまま流用）
- **GitHub OAuth**：フロントから受け取った認可コードをアクセストークンに交換し、ユーザーを DB に登録・更新
- **PostgreSQL への永続化**（MyBatis によるマッパー実装。テーブル定義は [`src/main/resources/db/goal_schema.sql`](./src/main/resources/db/goal_schema.sql)）

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
| GET  | `/goals` | 目標一覧を取得（達成率込み） |
| POST | `/goal_insert` / `/goal_update/{id}` / `/goal_delete/{id}` | 目標の登録・更新・削除 |
| GET  | `/action_plans` | アクションプラン一覧を取得（達成率込み） |
| POST | `/action_plan_insert` / `/action_plan_update/{id}` / `/action_plan_delete/{id}` | アクションプランの登録・更新・削除 |
| POST | `/action_plan_reorder` | ドラッグ&ドロップ確定後の優先順位一括更新 |
| GET  | `/notes` | メモ一覧を取得（学習用／タスク用／通常） |
| POST | `/note_insert` / `/note_update/{id}` / `/note_delete/{id}` | メモの登録・更新・削除 |
| POST | `/note_attach/{id}` | 未紐付けメモをアクションプランへ後から紐付け |
| POST | `/note_todo_toggle/{id}` | タスク用メモのtodoチェック切替 |
| GET  | `/category_list` / `/tag_list` | カテゴリ・タグ一覧 |
| POST | `/github/callback` | GitHub OAuth コールバック（コード→トークン交換＋ユーザー登録） |

## 🏗 構成

```
src/main/java/com/udemy/hello/
├── LearningApplication.java     … エントリポイント
├── GoalController.java          … 目標の REST コントローラ
├── ActionPlanController.java    … アクションプランの REST コントローラ
├── NoteController.java          … メモの REST コントローラ
├── LearningController.java      … カテゴリ／タグ・GitHub連携先切替・お問い合わせ等の共通API
├── GitHubAuthController.java    … GitHub OAuth コールバック
├── mapper/                      … Service / MyBatis Mapper（ProgressServiceが進捗集計を担当）
└── model/                       … エンティティ（Goal, ActionPlan, Note, NoteTodoItem 等）
src/main/resources/
├── application.properties       … 設定（秘密情報は環境変数から注入）
├── db/goal_schema.sql           … 目標達成アプリ用テーブルのDDL・移行スクリプト
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

## 💡 制作の背景

「学習メモ」と「GitHub 上のコード」を結びつけて残せる学習ログアプリを、
フロントエンドからバックエンドまで一貫して自作しました。
本バックエンドでは、REST API 設計・DB 設計（多対多のタグ管理）・OAuth のトークン交換など、
実務でも用いられる構成を Java / Spring Boot で実装しています。
