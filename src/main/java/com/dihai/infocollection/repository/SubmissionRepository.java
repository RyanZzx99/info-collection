package com.dihai.infocollection.repository;

import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByCollectionKeyOrderByCreatedAtDesc(CollectionKey collectionKey);

    long countByCollectionKey(CollectionKey collectionKey);

    boolean existsByCollectionKeyAndCbAccountIgnoreCaseAndStudentNameIgnoreCaseAndSubjectIgnoreCase(
        CollectionKey collectionKey,
        String cbAccount,
        String studentName,
        String subject
    );
}
