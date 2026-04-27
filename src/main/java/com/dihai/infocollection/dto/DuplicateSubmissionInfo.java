package com.dihai.infocollection.dto;

public class DuplicateSubmissionInfo {

    private final int rowNumber;
    private final String studentName;
    private final String examTimeBeijing;
    private final String examLocationCountry;
    private final String cbAccount;
    private final String subject;
    private final String operationType;
    private final String testCenter;
    private final String reason;
    private final SubmissionForm form;

    private DuplicateSubmissionInfo(int rowNumber, SubmissionForm form, String reason) {
        this.rowNumber = rowNumber;
        this.studentName = clean(form.getStudentName());
        this.examTimeBeijing = clean(form.getExamTimeBeijing());
        this.examLocationCountry = clean(form.getExamLocationCountry());
        this.cbAccount = clean(form.getCbAccount());
        this.subject = clean(form.getSubject());
        this.operationType = clean(form.getOperationType());
        this.testCenter = clean(form.getTestCenter());
        this.reason = reason;
        this.form = form;
    }

    public static DuplicateSubmissionInfo of(int rowNumber, SubmissionForm form, String reason) {
        return new DuplicateSubmissionInfo(rowNumber, form, reason);
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getExamTimeBeijing() {
        return examTimeBeijing;
    }

    public String getExamLocationCountry() {
        return examLocationCountry;
    }

    public String getCbAccount() {
        return cbAccount;
    }

    public String getSubject() {
        return subject;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getTestCenter() {
        return testCenter;
    }

    public String getReason() {
        return reason;
    }

    public SubmissionForm getForm() {
        return form;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
