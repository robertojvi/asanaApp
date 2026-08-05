package com.accessparks.asanaapp.model;

public enum Role {
    SUPER_USER, // full access, including user/profile management
    ADMIN,      // everything SUPER_USER has except user/profile management
    USER        // read-only: view projects and reports
}
