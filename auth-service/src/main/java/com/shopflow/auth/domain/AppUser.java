package com.shopflow.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A registered user, mapped to the app_users table.
 * ("user" is reserved in Postgres, hence the name.)
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the raw password. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    /** Single role per user, included in the token as a claim. */
    @Column(nullable = false)
    private String role = "CUSTOMER";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
        // required by JPA
    }

    public AppUser(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
