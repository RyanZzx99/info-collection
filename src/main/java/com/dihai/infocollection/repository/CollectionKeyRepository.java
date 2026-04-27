package com.dihai.infocollection.repository;

import com.dihai.infocollection.model.CollectionKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectionKeyRepository extends JpaRepository<CollectionKey, Long> {

    Optional<CollectionKey> findByCollectionKey(String collectionKey);

    boolean existsByCollectionKey(String collectionKey);
}
