
package com.smartdelivery.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@ComponentScan(basePackages = {
    "com.smartdelivery.delivery",
    "com.smartdelivery.dispatcher",
    "com.smartdelivery.tracking",
    "com.smartdelivery.notification",
    "com.smartdelivery.routeoptimizer"
})
public class TestConfig {
    // Configuration spécifique pour les tests de bout en bout
    // Peut inclure des mocks ou des configurations particulières
}
