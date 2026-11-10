package com.wersiv.datingsite.backend.profile.repository;

import com.wersiv.datingsite.backend.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    boolean existsByHandle(String handle);

    Optional<Profile> findByHandle(String handle);
}
