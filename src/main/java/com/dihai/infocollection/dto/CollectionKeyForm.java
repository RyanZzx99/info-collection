package com.dihai.infocollection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CollectionKeyForm {

    @NotBlank(message = "key 必填")
    @Size(max = 80, message = "key 最多 80 个字符")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$", message = "key 只能使用字母、数字、下划线和横线")
    private String collectionKey;

    @NotBlank(message = "名称必填")
    @Size(max = 120, message = "名称最多 120 个字符")
    private String name;

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
}
