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
        // ログイン(/github/**, /google/callback)・ヘルスチェック(/ping)・
        // 未ログイン訪問者も送れるお問い合わせ(/inquiry_submit)以外の
        // 全エンドポイントで本人確認を必須にする。
        // /google/refreshはapp_tokenでの本人確認が必要なため対象外にしない（/google/**の
        // ワイルドカードではなく/google/callbackだけを個別に除外している点に注意）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/github/**", "/google/callback", "/ping", "/inquiry_submit");
    }
}
