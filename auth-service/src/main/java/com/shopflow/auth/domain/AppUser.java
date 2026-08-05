package com.shopflow.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA ENTITY - maps this class to the "app_users" table.
 *
 * JPA vs SPRING DATA JPA (classic interview question):
 *  - JPA is only a SPECIFICATION (annotations + EntityManager API).
 *  - Hibernate is the IMPLEMENTATION Spring Boot auto-configures.
 *  - Spring Data JPA sits ON TOP and generates repository implementations
 *    from interface method names (see UserRepository).
 *
 * Named app_users because "user" is a reserved word in Postgres.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash - NEVER the raw password. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    /** Simple single-role model; embedded into the JWT as a claim. */
    @Column(nullable = false)
    private String role = "CUSTOMER";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
        // JPA requires a no-arg constructor (Hibernate instantiates via reflection)
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
