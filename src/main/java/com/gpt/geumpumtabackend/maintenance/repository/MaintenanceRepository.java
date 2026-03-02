package com.gpt.geumpumtabackend.maintenance.repository;

import com.gpt.geumpumtabackend.maintenance.domain.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
}
