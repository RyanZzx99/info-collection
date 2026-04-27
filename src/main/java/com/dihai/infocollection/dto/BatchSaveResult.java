package com.dihai.infocollection.dto;

import com.dihai.infocollection.model.Submission;

import java.util.ArrayList;
import java.util.List;

public class BatchSaveResult {

    private final List<Submission> savedSubmissions = new ArrayList<>();
    private final List<DuplicateSubmissionInfo> duplicateSubmissions = new ArrayList<>();

    public List<Submission> getSavedSubmissions() {
        return savedSubmissions;
    }

    public List<DuplicateSubmissionInfo> getDuplicateSubmissions() {
        return duplicateSubmissions;
    }

    public int getSavedCount() {
        return savedSubmissions.size();
    }

    public int getDuplicateCount() {
        return duplicateSubmissions.size();
    }

    public boolean hasDuplicates() {
        return !duplicateSubmissions.isEmpty();
    }

    public List<SubmissionForm> getDuplicateForms() {
        return duplicateSubmissions.stream()
            .map(DuplicateSubmissionInfo::getForm)
            .toList();
    }

    public void addSavedSubmission(Submission submission) {
        savedSubmissions.add(submission);
    }

    public void addDuplicateSubmission(DuplicateSubmissionInfo duplicateSubmission) {
        duplicateSubmissions.add(duplicateSubmission);
    }
}
