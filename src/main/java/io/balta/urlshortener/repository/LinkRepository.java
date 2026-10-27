package io.balta.urlshortener.repository;

import io.balta.urlshortener.domain.LinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<LinkEntity, Long> {

    @Query("SELECT l FROM LinkEntity l WHERE l.code = :code")
    Optional<LinkEntity> findByCode(@Param("code") String code);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LinkEntity l WHERE l.code = :code")
    boolean existsByCode(@Param("code") String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE LinkEntity l SET l.accessCount = l.accessCount + 1 WHERE l.code = :code")
    int incrementAccessCount(@Param("code") String code);
}

