package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.SubmissionForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalLanguageParserTest {

    private final NaturalLanguageParser parser = new NaturalLanguageParser(2026);

    @Test
    void parsesChineseMonthDayTimeAndChineseSubjectAlias() {
        SubmissionForm form = parser.parse("小王，zzxzzx@126.com,五月四日上午八点,澳门，英语");

        assertThat(form.getStudentName()).isEqualTo("小王");
        assertThat(form.getCbAccount()).isEqualTo("zzxzzx@126.com");
        assertThat(form.getExamTimeBeijing()).isEqualTo("2026-05-04T08:00");
        assertThat(form.getExamLocationCountry()).isEqualTo("澳门");
        assertThat(form.getSubject()).isEqualTo("English Language and Composition");
    }

    @Test
    void parsesChineseHalfHourTime() {
        SubmissionForm form = parser.parse("李四，5月4日下午2点半，香港，英语文学");

        assertThat(form.getExamTimeBeijing()).isEqualTo("2026-05-04T14:30");
        assertThat(form.getSubject()).isEqualTo("English Literature and Composition");
    }

    @Test
    void parsesBatchBySemicolon() {
        var rows = parser.parseBatch(
            "小王，zzxzzx@126.com,五月四日上午八点,澳门，英语;李四，lisi@example.com，5月4日下午2点半，香港，Biology"
        );

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getStudentName()).isEqualTo("小王");
        assertThat(rows.get(0).getExamTimeBeijing()).isEqualTo("2026-05-04T08:00");
        assertThat(rows.get(0).getSubject()).isEqualTo("English Language and Composition");
        assertThat(rows.get(1).getStudentName()).isEqualTo("李四");
        assertThat(rows.get(1).getExamTimeBeijing()).isEqualTo("2026-05-04T14:30");
        assertThat(rows.get(1).getSubject()).isEqualTo("Biology");
    }
}
