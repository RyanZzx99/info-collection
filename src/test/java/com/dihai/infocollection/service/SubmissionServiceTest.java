package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.SubmissionForm;
import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.model.SourceType;
import com.dihai.infocollection.model.Submission;
import com.dihai.infocollection.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmissionServiceTest {

    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final SubmissionService submissionService = new SubmissionService(submissionRepository);
    private final CollectionKey collectionKey = new CollectionKey();

    @BeforeEach
    void setUp() {
        when(submissionRepository.save(any(Submission.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsDuplicateCbNameSubjectAlreadyStored() {
        when(submissionRepository.existsByCollectionKeyAndCbAccountIgnoreCaseAndStudentNameIgnoreCaseAndSubjectIgnoreCase(
            collectionKey,
            "same@example.com",
            "小王",
            "biology"
        )).thenReturn(true);

        assertThatThrownBy(() -> submissionService.save(collectionKey, form("same@example.com", "小王", "Biology"), SourceType.FORM))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("信息已存在");
    }

    @Test
    void saveAllSkipsStoredDuplicateAndSavesOtherRows() {
        when(submissionRepository.existsByCollectionKeyAndCbAccountIgnoreCaseAndStudentNameIgnoreCaseAndSubjectIgnoreCase(
            collectionKey,
            "same@example.com",
            "小王",
            "biology"
        )).thenReturn(true);

        var result = submissionService.saveAll(
            collectionKey,
            List.of(
                form("same@example.com", "小王", "Biology"),
                form("new@example.com", "小李", "Chemistry")
            )
        );

        assertThat(result.getSavedCount()).isEqualTo(1);
        assertThat(result.getDuplicateCount()).isEqualTo(1);
        assertThat(result.getDuplicateSubmissions().getFirst().getReason()).contains("库中已存在");
    }

    @Test
    void saveAllSkipsDuplicateCbNameSubjectInSameBatch() {
        var result = submissionService.saveAll(
            collectionKey,
            List.of(
                form("same@example.com", "小王", "Biology"),
                form("SAME@example.com", "小王", "biology")
            )
        );

        assertThat(result.getSavedCount()).isEqualTo(1);
        assertThat(result.getDuplicateCount()).isEqualTo(1);
        assertThat(result.getDuplicateSubmissions().getFirst().getReason()).contains("本次提交");
    }

    @Test
    void saveAllAllowsSameCbAndNameForDifferentSubjects() {
        var result = submissionService.saveAll(
            collectionKey,
            List.of(
                form("same@example.com", "小王", "Biology"),
                form("SAME@example.com", "小王", "Chemistry")
            )
        );

        assertThat(result.getSavedCount()).isEqualTo(2);
        assertThat(result.getDuplicateCount()).isZero();
    }

    private SubmissionForm form(String cbAccount, String studentName, String subject) {
        SubmissionForm form = new SubmissionForm();
        form.setStudentName(studentName);
        form.setExamTimeBeijing("2026-05-04T08:00");
        form.setExamLocationCountry("澳门");
        form.setCbAccount(cbAccount);
        form.setSubject(subject);
        form.setOperationType("2小时网面全题");
        return form;
    }
}
