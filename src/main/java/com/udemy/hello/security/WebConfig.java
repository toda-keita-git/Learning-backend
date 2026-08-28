package com.udemy.hello.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ログイン(/github/callback, /google/callback)・ヘルスチェック(/ping)・
        // 未ログイン訪問者も送れるお問い合わせ(/inquiry_submit)以外の
        // 全エンドポイントで本人確認を必須にする。
        // /google/refresh・/github/link・/google/linkはapp_tokenでの本人確認が必要なため
        // 対象外にしない（/github/**や/google/**のワイルドカードではなく、
        // ログイン用の/callbackだけを個別に除外している点に注意。
        // 連携は「誰に連携するのか」をトークンで決めるため、必ず認証が要る）
        //
        // /error は必ず除外すること。Springは404や例外発生時に内部で/errorへフォワードするが、
        // このフォワードもDispatcherServletを通るためインターセプターが動いてしまう。
        // /errorを除外しないと、Authorizationヘッダーが引き継がれないせいで
        // 「本当は404や500なのに401が返る」状態になり、原因究明が著しく困難になる
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/github/callback", "/google/callback", "/ping", "/inquiry_submit", "/error");
    }
}
