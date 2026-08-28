package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.enums.VenueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    Page<Venue> findByOwnerIdAndDeletedFalse(Long ownerId, Pageable pageable);

    Page<Venue> findByDeletedFalse(Pageable pageable);

    Page<Venue> findByStatusAndDeletedFalse(VenueStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "amenities"})
    Optional<Venue> findByIdAndDeletedFalse(Long id);

    long countByStatus(VenueStatus status);
}
