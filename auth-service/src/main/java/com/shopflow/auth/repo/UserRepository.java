package com.shopflow.auth.repo;

import com.shopflow.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * SPRING DATA JPA repository - no implementation class anywhere!
 *
 * Spring Data parses the METHOD NAME at startup and generates the query:
 *   findByEmail  ->  SELECT u FROM AppUser u WHERE u.email = ?1
 * JpaRepository also contributes save/findById/findAll/delete + paging/sorting.
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
