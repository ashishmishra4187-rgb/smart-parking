package com.parking.backend.gate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryExitLogRepository extends JpaRepository<EntryExitLog, Long> {
}