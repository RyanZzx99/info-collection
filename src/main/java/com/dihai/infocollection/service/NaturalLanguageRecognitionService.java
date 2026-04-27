package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.SubmissionForm;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NaturalLanguageRecognitionService {

    private final DeepSeekExtractionService deepSeekExtractionService;
    private final NaturalLanguageParser naturalLanguageParser;

    public NaturalLanguageRecognitionService(
        DeepSeekExtractionService deepSeekExtractionService,
        NaturalLanguageParser naturalLanguageParser
    ) {
        this.deepSeekExtractionService = deepSeekExtractionService;
        this.naturalLanguageParser = naturalLanguageParser;
    }

    public List<SubmissionForm> recognizeBatch(String rawText) {
        List<SubmissionForm> aiRows = deepSeekExtractionService.extractBatch(rawText);
        if (!aiRows.isEmpty()) {
            return aiRows;
        }
        return naturalLanguageParser.parseBatch(rawText);
    }

    public boolean isAiConfigured() {
        return deepSeekExtractionService.isConfigured();
    }
}
