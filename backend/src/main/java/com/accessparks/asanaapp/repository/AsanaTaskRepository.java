package com.accessparks.asanaapp.repository;

import com.accessparks.asanaapp.model.AsanaTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsanaTaskRepository extends JpaRepository<AsanaTask, String> {
    List<AsanaTask> findByProjectGidIn(List<String> projectGids);
}
