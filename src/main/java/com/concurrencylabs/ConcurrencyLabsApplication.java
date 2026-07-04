package com.concurrencylabs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 高併發情境 lab 主程式。
 *
 * 75 個情境各自獨立成 com.concurrencylabs.&lt;scenario&gt; 的 flat package。
 * 「遇到狀況該看哪個 package」的導覽索引見 repo 根目錄 ROUTE.md。
 */
@SpringBootApplication
public class ConcurrencyLabsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcurrencyLabsApplication.class, args);
    }
}
