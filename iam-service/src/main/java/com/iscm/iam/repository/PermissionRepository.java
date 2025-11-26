package com.iscm.iam.repository;

import com.iscm.iam.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    Boolean existsByCode(String code);

    @Query("SELECT p FROM Permission p WHERE p.code IN :codes")
    List<Permission> findByCodeIn(@Param("codes") List<String> codes);
}