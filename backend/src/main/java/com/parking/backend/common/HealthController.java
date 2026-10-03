package com.parking.backend.common;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("status", "UP", "database", one == 1 ? "UP" : "DOWN");
    }
}