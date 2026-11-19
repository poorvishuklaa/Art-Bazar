package com.artbazaar.repository;

import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Admin user search; {@code pattern} is a lower-case LIKE pattern (escape char '!') or null. */
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:pattern is null or lower(u.name) like :pattern escape '!' or lower(u.email) like :pattern escape '!')
            """)
    Page<User> search(@Param("role") Role role, @Param("pattern") String pattern, Pageable pageable);

    @Query("select u.role, count(u) from User u group by u.role")
    List<Object[]> countByRole();
}
