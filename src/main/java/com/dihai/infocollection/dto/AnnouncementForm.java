package com.dihai.infocollection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AnnouncementForm {

    @NotBlank(message = "页面说明文字不能为空")
    @Size(max = 4000, message = "页面说明文字最多 4000 个字符")
    private String announcementText;

    public String getAnnouncementText() {
        return announcementText;
    }

    public void setAnnouncementText(String announcementText) {
        this.announcementText = announcementText;
    }

    public static AnnouncementForm of(String announcementText) {
        AnnouncementForm form = new AnnouncementForm();
        form.setAnnouncementText(announcementText);
        return form;
    }
}
