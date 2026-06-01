package org.xhy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 论文返修智能Agent平台 - 应用入口 */
@SpringBootApplication
@EnableScheduling
public class PaperRevisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperRevisionApplication.class, args);
    }
}
