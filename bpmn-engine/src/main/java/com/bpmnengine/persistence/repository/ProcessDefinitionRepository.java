package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.ProcessDefinitionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinitionEntity, String> {

    // ── Latest / by key ───────────────────────────────────────────────────────

    /** Latest version of a process key (regardless of active/suspended). */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.key = :key ORDER BY d.version DESC LIMIT 1")
    Optional<ProcessDefinitionEntity> findLatestByKey(@Param("key") String key);

    /** Same query used during deployment to find the current max version. */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.key = :key ORDER BY d.version DESC LIMIT 1")
    Optional<ProcessDefinitionEntity> findLatestVersionByKey(@Param("key") String key);

    /** All versions for a given key, newest first. */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.key = :key ORDER BY d.version DESC")
    List<ProcessDefinitionEntity> findAllVersionsByKey(@Param("key") String key);

    /** A specific version of a key. */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.key = :key AND d.version = :version")
    Optional<ProcessDefinitionEntity> findByKeyAndVersion(@Param("key") String key,
                                                           @Param("version") int version);

    // ── Active version ────────────────────────────────────────────────────────

    /** The single active version for a key (used when starting by key). */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.key = :key AND d.active = true")
    Optional<ProcessDefinitionEntity> findActiveByKey(@Param("key") String key);

    // ── Listing ───────────────────────────────────────────────────────────────

    /** All distinct process keys — one row per key (the active version). */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.active = true ORDER BY d.key")
    List<ProcessDefinitionEntity> findAllActiveDefinitions();

    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.active = true ORDER BY d.key")
    Page<ProcessDefinitionEntity> findAllActiveDefinitions(Pageable pageable);

    /** Definitions whose name or key contains the search string (case-insensitive). */
    @Query("SELECT d FROM ProcessDefinitionEntity d WHERE d.active = true " +
           "AND (LOWER(d.key) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<ProcessDefinitionEntity> searchActive(@Param("q") String query);

    List<ProcessDefinitionEntity> findByKey(String key);
    List<ProcessDefinitionEntity> findBySuspended(boolean suspended);

    // ── Bulk updates ──────────────────────────────────────────────────────────

    /** Clear the active flag for all versions of a key. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProcessDefinitionEntity d SET d.active = false WHERE d.key = :key")
    void clearActiveForKey(@Param("key") String key);

    /** Set active flag on one specific version, clearing all others for that key in one shot. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProcessDefinitionEntity d SET d.active = (d.version = :version) WHERE d.key = :key")
    void setActiveVersion(@Param("key") String key, @Param("version") int version);

    /** Suspend / unsuspend all versions of a key at once. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProcessDefinitionEntity d SET d.suspended = :suspended WHERE d.key = :key")
    void setSuspendedForKey(@Param("key") String key, @Param("suspended") boolean suspended);
}
