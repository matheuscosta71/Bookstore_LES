package com.matheusgn.ecommerce.common.code;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, CodeSequenceScope> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodeSequence c where c.scope = :scope")
    Optional<CodeSequence> findByScopeForUpdate(@Param("scope") CodeSequenceScope scope);
}
