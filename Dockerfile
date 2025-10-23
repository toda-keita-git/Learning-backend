# ベースイメージ（Java 17）
FROM eclipse-temurin:17-jdk

# アプリJARをコピー
COPY target/*.jar app.jar

# Renderが指定するPORT環境変数を利用
ENV PORT 8080
EXPOSE 8080

# 実行コマンド
ENTRYPOINT ["java", "-jar", "/app.jar"]
