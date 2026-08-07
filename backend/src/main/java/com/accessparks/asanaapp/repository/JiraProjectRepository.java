package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.JiraProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraProjectRepository extends JpaRepository<JiraProject, String> {
}
