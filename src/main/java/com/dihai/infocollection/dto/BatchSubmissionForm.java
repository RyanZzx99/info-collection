package com.dihai.infocollection.dto;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class BatchSubmissionForm {

    @Valid
    private List<SubmissionForm> rows = new ArrayList<>();

    public List<SubmissionForm> getRows() {
        return rows;
    }

    public void setRows(List<SubmissionForm> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }

    public static BatchSubmissionForm withBlankRow() {
        BatchSubmissionForm form = new BatchSubmissionForm();
        form.getRows().add(new SubmissionForm());
        return form;
    }

    public static BatchSubmissionForm of(List<SubmissionForm> rows) {
        BatchSubmissionForm form = new BatchSubmissionForm();
        form.setRows(rows);
        if (form.getRows().isEmpty()) {
            form.getRows().add(new SubmissionForm());
        }
        return form;
    }
}
