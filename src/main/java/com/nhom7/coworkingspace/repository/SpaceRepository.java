package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Space;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Space} entity supporting dynamic Specification queries.
 */
@Repository
public interface SpaceRepository extends JpaRepository<Space, Long>, JpaSpecificationExecutor<Space> {

    @Override
    @EntityGraph(attributePaths = {"venue"})
    Page<Space> findAll(Specification<Space> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Space s WHERE s.id = :id")
    Optional<Space> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"venue", "hosts"})
    List<Space> findByVenueId(Long venueId);

    @EntityGraph(attributePaths = {"venue"})
    Page<Space> findByVenueIdAndVenueDeletedFalse(Long venueId, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Space s LEFT JOIN s.hosts h WHERE (s.venue.owner.email = :email OR h.email = :email) AND s.venue.deleted = false")
    @EntityGraph(attributePaths = {"venue"})
    Page<Space> findMySpaces(@Param("email") String email, Pageable pageable);
}
