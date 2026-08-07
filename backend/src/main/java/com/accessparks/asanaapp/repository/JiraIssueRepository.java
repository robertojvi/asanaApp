package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.JiraIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JiraIssueRepository extends JpaRepository<JiraIssue, String> {
    List<JiraIssue> findByProjectIdInOrderByKeyAsc(List<String> projectIds);
}
