package com.dihai.infocollection.dto;

import com.dihai.infocollection.model.Submission;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {

    private int totalRows;
    private int importedRows;
    private final List<String> errors = new ArrayList<>();
    private final List<Submission> importedSubmissions = new ArrayList<>();

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(int importedRows) {
        this.importedRows = importedRows;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<Submission> getImportedSubmissions() {
        return importedSubmissions;
    }

    public void addImportedSubmission(Submission submission) {
        importedSubmissions.add(submission);
    }
}
