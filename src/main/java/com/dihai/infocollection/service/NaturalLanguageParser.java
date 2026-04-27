package com.dihai.infocollection.service;

import com.dihai.infocollection.dto.SubmissionForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NaturalLanguageParser {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern EXPLICIT_NAME_PATTERN =
        Pattern.compile("(?:姓名|学生|名字)[:：\\s]*([\\p{IsHan}A-Za-z·]{2,30})");

    private static final Pattern YEAR_TIME_PATTERN =
        Pattern.compile("(\\d{4})[年/.-](\\d{1,2})[月/.-](\\d{1,2})日?\\s*(上午|早上|中午|下午|晚上)?\\s*(\\d{1,2})(?:[:：点时](半|\\d{1,2}|[一二三四五六七八九十]{1,3}分?)?)?");

    private static final Pattern MONTH_TIME_PATTERN =
        Pattern.compile("(\\d{1,2})月(\\d{1,2})日\\s*(上午|早上|中午|下午|晚上)?\\s*(\\d{1,2})(?:[:：点时](半|\\d{1,2}|[一二三四五六七八九十]{1,3}分?)?)?");

    private static final Pattern CHINESE_MONTH_TIME_PATTERN =
        Pattern.compile("([一二三四五六七八九十]{1,3})月([一二三四五六七八九十]{1,3})[日号]?\\s*(上午|早上|中午|下午|晚上)?\\s*([一二三四五六七八九十]{1,3})(?:[点时](半|[一二三四五六七八九十]{1,3}分?)?)?");

    private static final Pattern TEST_CENTER_PATTERN =
        Pattern.compile("(?:具体考点|考点|地点)[:：\\s]*([^，,。;；\\n]+)");

    private static final List<String> LOCATIONS = List.of(
        "香港", "澳门", "台湾", "新加坡", "日本", "韩国", "泰国", "马来西亚",
        "美国", "加拿大", "英国", "澳大利亚", "越南", "菲律宾", "印度尼西亚"
    );

    private static final List<SubjectAlias> SUBJECT_ALIASES = List.of(
        new SubjectAlias("英语文学", "English Literature and Composition"),
        new SubjectAlias("英文文学", "English Literature and Composition"),
        new SubjectAlias("英语语言", "English Language and Composition"),
        new SubjectAlias("英文语言", "English Language and Composition"),
        new SubjectAlias("英语写作", "English Language and Composition"),
        new SubjectAlias("英文写作", "English Language and Composition"),
        new SubjectAlias("英语", "English Language and Composition"),
        new SubjectAlias("英文", "English Language and Composition"),
        new SubjectAlias("生物", "Biology"),
        new SubjectAlias("欧洲历史", "European History"),
        new SubjectAlias("微积分AB", "Calculus AB"),
        new SubjectAlias("微积分BC", "Calculus BC"),
        new SubjectAlias("化学", "Chemistry"),
        new SubjectAlias("统计", "Statistics"),
        new SubjectAlias("心理", "Psychology"),
        new SubjectAlias("宏观经济", "Macroeconomics"),
        new SubjectAlias("微观经济", "Microeconomics"),
        new SubjectAlias("美国历史", "United States History"),
        new SubjectAlias("世界历史", "World History: Modern")
    );

    private final int defaultExamYear;

    public NaturalLanguageParser(@Value("${app.default-exam-year:2026}") int defaultExamYear) {
        this.defaultExamYear = defaultExamYear;
    }

    public SubmissionForm parse(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        SubmissionForm form = new SubmissionForm();
        form.setRawText(text);
        form.setCbAccount(findEmail(text));
        form.setStudentName(findName(text));
        form.setExamTimeBeijing(findExamTime(text));
        form.setExamLocationCountry(findLocation(text));
        form.setSubject(findSubject(text));
        form.setOperationType(findOperationType(text));
        form.setTestCenter(findTestCenter(text));
        return form;
    }

    public List<SubmissionForm> parseBatch(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isBlank()) {
            return List.of(new SubmissionForm());
        }

        List<SubmissionForm> rows = Pattern.compile("[;；]")
            .splitAsStream(text)
            .map(String::trim)
            .filter(part -> !part.isBlank())
            .map(this::parse)
            .toList();

        return rows.isEmpty() ? List.of(new SubmissionForm()) : rows;
    }

    private String findEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private String findName(String text) {
        Matcher explicit = EXPLICIT_NAME_PATTERN.matcher(text);
        if (explicit.find()) {
            return explicit.group(1).trim();
        }

        String[] chunks = text.split("[，,。;；\\n\\s]+");
        for (String chunk : chunks) {
            String candidate = chunk.trim();
            if (candidate.matches("[\\p{IsHan}A-Za-z·]{2,20}")
                && LOCATIONS.stream().noneMatch(candidate::contains)
                && FormOptions.OPERATION_TYPES.stream().noneMatch(candidate::contains)) {
                return candidate;
            }
        }
        return "";
    }

    private String findExamTime(String text) {
        Matcher yearMatcher = YEAR_TIME_PATTERN.matcher(text);
        if (yearMatcher.find()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            int month = Integer.parseInt(yearMatcher.group(2));
            int day = Integer.parseInt(yearMatcher.group(3));
            String period = yearMatcher.group(4);
            int hour = normalizeHour(period, Integer.parseInt(yearMatcher.group(5)));
            int minute = parseMinute(yearMatcher.group(6));
            return String.format("%04d-%02d-%02dT%02d:%02d", year, month, day, hour, minute);
        }

        Matcher monthMatcher = MONTH_TIME_PATTERN.matcher(text);
        if (monthMatcher.find()) {
            int month = Integer.parseInt(monthMatcher.group(1));
            int day = Integer.parseInt(monthMatcher.group(2));
            String period = monthMatcher.group(3);
            int hour = normalizeHour(period, Integer.parseInt(monthMatcher.group(4)));
            int minute = parseMinute(monthMatcher.group(5));
            return String.format("%04d-%02d-%02dT%02d:%02d", defaultExamYear, month, day, hour, minute);
        }

        Matcher chineseMonthMatcher = CHINESE_MONTH_TIME_PATTERN.matcher(text);
        if (chineseMonthMatcher.find()) {
            int month = parseChineseNumber(chineseMonthMatcher.group(1));
            int day = parseChineseNumber(chineseMonthMatcher.group(2));
            String period = chineseMonthMatcher.group(3);
            int hour = normalizeHour(period, parseChineseNumber(chineseMonthMatcher.group(4)));
            int minute = parseChineseMinute(chineseMonthMatcher.group(5));
            return String.format("%04d-%02d-%02dT%02d:%02d", defaultExamYear, month, day, hour, minute);
        }

        return "";
    }

    private String findLocation(String text) {
        return LOCATIONS.stream()
            .filter(text::contains)
            .findFirst()
            .orElse("");
    }

    private String findSubject(String text) {
        String lowered = text.toLowerCase(Locale.ROOT);
        String matchedSubject = FormOptions.AP_SUBJECTS.stream()
            .filter(option -> lowered.contains(option.toLowerCase(Locale.ROOT)))
            .findFirst()
            .orElse("");
        if (!matchedSubject.isBlank()) {
            return matchedSubject;
        }

        return SUBJECT_ALIASES.stream()
            .filter(alias -> text.contains(alias.alias()))
            .map(SubjectAlias::subject)
            .findFirst()
            .orElse("");
    }

    private String findOperationType(String text) {
        for (String operationType : FormOptions.OPERATION_TYPES) {
            if (text.contains(operationType)) {
                return operationType;
            }
        }
        if (text.contains("面授") || text.contains("线下")) {
            return "4小时线下面授";
        }
        if (text.contains("网面") || text.contains("网课")) {
            return "2小时网面全题";
        }
        if (text.contains("基础水印")) {
            return "基础水印（选择题+大题提示）";
        }
        if (text.contains("混合水印")) {
            return "混合水印（2小时网面大题）";
        }
        if (text.contains("高级水印") || text.contains("保5")) {
            return "高级水印（保5/仅限境外）";
        }
        return "";
    }

    private String findTestCenter(String text) {
        Matcher matcher = TEST_CENTER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private int normalizeHour(String period, int hour) {
        if (period == null) {
            return hour;
        }
        if (("下午".equals(period) || "晚上".equals(period)) && hour < 12) {
            return hour + 12;
        }
        if ("中午".equals(period) && hour < 11) {
            return hour + 12;
        }
        return hour;
    }

    private int parseMinute(String minute) {
        if (minute == null || minute.isBlank()) {
            return 0;
        }
        if ("半".equals(minute)) {
            return 30;
        }
        String cleaned = minute.replace("分", "");
        if (cleaned.matches("\\d{1,2}")) {
            return Integer.parseInt(cleaned);
        }
        return parseChineseNumber(cleaned);
    }

    private int parseChineseMinute(String minute) {
        if (minute == null || minute.isBlank()) {
            return 0;
        }
        if ("半".equals(minute)) {
            return 30;
        }
        return parseChineseNumber(minute.replace("分", ""));
    }

    private int parseChineseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String normalized = value.trim();
        if ("十".equals(normalized)) {
            return 10;
        }
        int tenIndex = normalized.indexOf('十');
        if (tenIndex >= 0) {
            String tens = normalized.substring(0, tenIndex);
            String ones = normalized.substring(tenIndex + 1);
            int tenValue = tens.isBlank() ? 1 : chineseDigit(tens.charAt(0));
            int oneValue = ones.isBlank() ? 0 : chineseDigit(ones.charAt(0));
            return tenValue * 10 + oneValue;
        }
        return chineseDigit(normalized.charAt(0));
    }

    private int chineseDigit(char value) {
        return switch (value) {
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> 0;
        };
    }

    private record SubjectAlias(String alias, String subject) {
    }
}
