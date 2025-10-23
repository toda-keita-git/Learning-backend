# ===== 1. ビルド用ステージ =====
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# プロジェクト全体をコピー
COPY . .

# Maven Wrapperに実行権限を付与
RUN chmod +x ./mvnw

# Mavenでビルド（target/*.jar を生成）
RUN ./mvnw clean package -DskipTests

# ===== 2. 実行用ステージ =====
FROM eclipse-temurin:17-jdk

WORKDIR /app

# RenderがPORTを環境変数として渡す
ENV PORT=8080
EXPOSE 8080

# ビルド成果物（jar）をコピー
COPY --from=builder /app/target/*.jar app.jar

# 起動コマンド
ENTRYPOINT ["java", "-jar", "/app.jar"]
