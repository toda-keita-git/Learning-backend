package com.udemy.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部の定期pingサービス（Google Apps Script等）からスリープ防止のために叩くための
 * 軽量エンドポイント。DBアクセスや認証を伴わない。
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }
}
