package com.parking.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GateIndexes implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public GateIndexes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // safety net under the code: at most one ACTIVE session per vehicle and per slot
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS one_active_session_per_vehicle "
                + "ON parking_session (vehicle_id) WHERE status = 'ACTIVE'");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS one_active_session_per_slot "
                + "ON parking_session (slot_id) WHERE status = 'ACTIVE'");
    }
}