package com.dihai.infocollection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NaturalLanguageForm {

    @NotBlank(message = "请粘贴需要识别的信息")
    @Size(max = 3000, message = "文本最多 3000 个字符")
    private String rawText;

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
