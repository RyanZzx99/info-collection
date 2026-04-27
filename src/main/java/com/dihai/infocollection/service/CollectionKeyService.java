package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.CollectionKeyForm;
import com.dihai.infocollection.model.CollectionKey;
import com.dihai.infocollection.repository.CollectionKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollectionKeyService {

    private final CollectionKeyRepository collectionKeyRepository;

    public CollectionKeyService(CollectionKeyRepository collectionKeyRepository) {
        this.collectionKeyRepository = collectionKeyRepository;
    }

    public List<CollectionKey> findAll() {
        return collectionKeyRepository.findAll();
    }

    public CollectionKey getEnabledByKey(String key) {
        CollectionKey collectionKey = getByKey(key);
        if (!collectionKey.isEnabled()) {
            throw new IllegalArgumentException("该收集链接已停用");
        }
        return collectionKey;
    }

    public CollectionKey getByKey(String key) {
        return collectionKeyRepository.findByCollectionKey(key)
            .orElseThrow(() -> new IllegalArgumentException("收集 key 不存在：" + key));
    }

    @Transactional
    public CollectionKey create(CollectionKeyForm form) {
        String key = normalize(form.getCollectionKey());
        if (collectionKeyRepository.existsByCollectionKey(key)) {
            throw new IllegalArgumentException("key 已存在：" + key);
        }

        CollectionKey collectionKey = new CollectionKey();
        collectionKey.setCollectionKey(key);
        collectionKey.setName(form.getName().trim());
        collectionKey.setEnabled(true);
        return collectionKeyRepository.save(collectionKey);
    }

    @Transactional
    public void toggle(String key) {
        CollectionKey collectionKey = getByKey(key);
        collectionKey.setEnabled(!collectionKey.isEnabled());
    }

    @Transactional
    public void updateAnnouncement(String key, String announcementText) {
        CollectionKey collectionKey = getByKey(key);
        collectionKey.setAnnouncementText(announcementText == null ? "" : announcementText.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
