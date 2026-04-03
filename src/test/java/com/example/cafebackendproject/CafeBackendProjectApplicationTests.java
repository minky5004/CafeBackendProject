package com.example.cafebackendproject;

import com.example.cafebackendproject.config.TestRedissonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedissonConfig.class)
class CafeBackendProjectApplicationTests {

    @Test
    void contextLoads() {
    }

}
