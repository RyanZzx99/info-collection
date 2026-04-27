package com.dihai.infocollection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "collection_keys",
    indexes = {
        @Index(name = "idx_collection_keys_collection_key", columnList = "collection_key", unique = true)
    }
)
public class CollectionKey {

    public static final String DEFAULT_ANNOUNCEMENT_TEXT = """
        各位同学/老师好：

        2026年 AP 考试现已开始收集报名信息，请大家尽快填写下方表格，并仔细核对所填内容，确保准确无误。

        本次可选择的操作类型共五种：

        4小时线下面授
        2小时网面全题
        基础水印（选择题 + 大题提示）
        混合水印（2小时网面大题）
        高级水印（保5 / 仅限境外）

        如报考多个科目，请每个科目单独填写一行，避免错填或混填。
        若选择“4小时线下面授”，还需额外填写具体考点信息。

        感谢大家的配合。
        """;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_key", nullable = false, unique = true, length = 80)
    private String collectionKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Lob
    @Column(name = "announcement_text")
    private String announcementText = DEFAULT_ANNOUNCEMENT_TEXT;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (announcementText == null || announcementText.isBlank()) {
            announcementText = DEFAULT_ANNOUNCEMENT_TEXT;
        }
    }

    public Long getId() {
        return id;
    }

    public String getCollectionKey() {
        return collectionKey;
    }

    public void setCollectionKey(String collectionKey) {
        this.collectionKey = collectionKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnnouncementText() {
        return announcementText == null || announcementText.isBlank()
            ? DEFAULT_ANNOUNCEMENT_TEXT
            : announcementText;
    }

    public void setAnnouncementText(String announcementText) {
        this.announcementText = announcementText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
