package com.dihai.infocollection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "submissions",
    indexes = {
        @Index(name = "idx_submissions_collection_key_id", columnList = "collection_key_id"),
        @Index(name = "idx_submissions_cb_account", columnList = "cb_account"),
        @Index(name = "idx_submissions_dedupe", columnList = "collection_key_id, cb_account, student_name, subject"),
        @Index(name = "idx_submissions_subject", columnList = "subject")
    }
)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_key_id", nullable = false)
    private CollectionKey collectionKey;

    @Column(name = "student_name", nullable = false, length = 80)
    private String studentName;

    @Column(name = "exam_time_beijing", nullable = false)
    private LocalDateTime examTimeBeijing;

    @Column(name = "exam_location_country", nullable = false, length = 120)
    private String examLocationCountry;

    @Column(name = "cb_account", nullable = false, length = 160)
    private String cbAccount;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(name = "operation_type", nullable = false, length = 160)
    private String operationType;

    @Column(name = "test_center", length = 200)
    private String testCenter;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType = SourceType.FORM;

    @Lob
    @Column(name = "raw_text")
    private String rawText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CollectionKey getCollectionKey() {
        return collectionKey;
    }

    public void setCollectionKey(CollectionKey collectionKey) {
        this.collectionKey = collectionKey;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public LocalDateTime getExamTimeBeijing() {
        return examTimeBeijing;
    }

    public void setExamTimeBeijing(LocalDateTime examTimeBeijing) {
        this.examTimeBeijing = examTimeBeijing;
    }

    public String getExamLocationCountry() {
        return examLocationCountry;
    }

    public void setExamLocationCountry(String examLocationCountry) {
        this.examLocationCountry = examLocationCountry;
    }

    public String getCbAccount() {
        return cbAccount;
    }

    public void setCbAccount(String cbAccount) {
        this.cbAccount = cbAccount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getTestCenter() {
        return testCenter;
    }

    public void setTestCenter(String testCenter) {
        this.testCenter = testCenter;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
