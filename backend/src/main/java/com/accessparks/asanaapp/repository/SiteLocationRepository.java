package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.SiteLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteLocationRepository extends JpaRepository<SiteLocation, Long> {
    List<SiteLocation> findBySubvenueIdInOrderByNameAsc(List<Long> subvenueIds);
}
