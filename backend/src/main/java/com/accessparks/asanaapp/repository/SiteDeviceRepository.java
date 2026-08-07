package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.SiteDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteDeviceRepository extends JpaRepository<SiteDevice, Long> {
    List<SiteDevice> findByLocationIdInOrderByDeviceNameAsc(List<Long> locationIds);
}
