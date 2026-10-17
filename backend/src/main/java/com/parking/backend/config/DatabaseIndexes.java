package com.parking.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexes implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DatabaseIndexes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS one_active_reservation_per_slot "
                + "ON reservation (slot_id) WHERE status = 'ACTIVE'");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS one_active_reservation_per_vehicle "
                + "ON reservation (vehicle_id) WHERE status = 'ACTIVE'");
    }
}