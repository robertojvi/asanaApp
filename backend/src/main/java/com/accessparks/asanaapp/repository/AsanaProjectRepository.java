package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.AsanaProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsanaProjectRepository extends JpaRepository<AsanaProject, String> {
}
