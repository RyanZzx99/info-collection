package com.dihai.infocollection.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmissionForm {

    @NotBlank(message = "姓名必填")
    @Size(max = 80, message = "姓名最多 80 个字符")
    private String studentName;

    @NotBlank(message = "开考时间必填")
    private String examTimeBeijing;

    @NotBlank(message = "考试地点必填")
    @Size(max = 120, message = "考试地点最多 120 个字符")
    private String examLocationCountry;

    @NotBlank(message = "CB 账号必填")
    @Email(message = "CB 账号请填写真实邮箱")
    @Size(max = 160, message = "CB 账号最多 160 个字符")
    private String cbAccount;

    @NotBlank(message = "科目必填")
    @Size(max = 120, message = "科目最多 120 个字符")
    private String subject;

    @NotBlank(message = "操作类型必填")
    @Size(max = 160, message = "操作类型最多 160 个字符")
    private String operationType;

    @Size(max = 200, message = "具体考点最多 200 个字符")
    private String testCenter;

    private String rawText;

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getExamTimeBeijing() {
        return examTimeBeijing;
    }

    public void setExamTimeBeijing(String examTimeBeijing) {
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

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
