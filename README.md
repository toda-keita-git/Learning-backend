# 学習記録アプリ（バックエンド）

学習ログ管理アプリの **バックエンド API** です。
Java / Spring Boot で実装し、学習記録の永続化・GitHub OAuth 認証・タグ／カテゴリ管理を担います。

> 🔗 フロントエンド（React / TypeScript）：https://github.com/toda-keita-git/Learning-frontend

---

## 📌 担当している役割

- **学習記録の CRUD API**（登録・取得・更新・削除）
- **タグ／カテゴリの管理**（多対多をリレーションで管理）
- **GitHub OAuth**：フロントから受け取った認可コードをアクセストークンに交換し、ユーザーを DB に登録・更新
- **PostgreSQL への永続化**（MyBatis によるマッパー実装）

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
| GET  | `/learning?user_id={id}` | ユーザーの学習記録一覧を取得 |
| POST | `/learning_insert` | 学習記録を登録（タグも同時に登録） |
| POST | `/learning_update/{id}` | 学習記録を更新 |
| POST | `/learning_delete/{id}` | 学習記録を削除 |
| GET  | `/category_list` | カテゴリ一覧 |
| GET  | `/tag_list` | タグ一覧 |
| POST | `/github/callback` | GitHub OAuth コールバック（コード→トークン交換＋ユーザー登録） |

## 🏗 構成

```
src/main/java/com/udemy/hello/
├── LearningApplication.java     … エントリポイント
├── LearningController.java      … 学習記録の REST コントローラ
├── GitHubAuthController.java    … GitHub OAuth コールバック
├── mapper/                      … Service / MyBatis Mapper
└── model/                       … エンティティ（Learning, tags, categories 等）
src/main/resources/
├── application.properties       … 設定（秘密情報は環境変数から注入）
└── mapper/*.xml                 … MyBatis の SQL 定義
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
