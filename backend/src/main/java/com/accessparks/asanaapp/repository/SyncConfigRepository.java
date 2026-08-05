package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.SyncConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncConfigRepository extends JpaRepository<SyncConfig, Long> {
}
