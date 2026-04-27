package com.dihai.infocollection.service;

import java.util.List;

public final class FormOptions {

    public static final List<String> OPERATION_TYPES = List.of(
        "4小时线下面授",
        "2小时网面全题",
        "基础水印（选择题+大题提示）",
        "混合水印（2小时网面大题）",
        "高级水印（保5/仅限境外）"
    );

    public static final List<String> EXAM_LOCATIONS = List.of(
        "香港",
        "澳门",
        "台湾",
        "新加坡",
        "日本",
        "韩国",
        "泰国",
        "马来西亚",
        "美国",
        "加拿大",
        "英国",
        "澳大利亚",
        "越南",
        "菲律宾",
        "印度尼西亚"
    );

    public static final List<String> AP_SUBJECTS = List.of(
        "2-D Art and Design",
        "3-D Art and Design",
        "Art History",
        "Biology",
        "Calculus AB",
        "Calculus BC",
        "Chemistry",
        "Chinese Language and Culture",
        "Comparative Government and Politics",
        "Computer Science A",
        "Computer Science Principles",
        "Drawing",
        "English Language and Composition",
        "English Literature and Composition",
        "Environmental Science",
        "European History",
        "French Language and Culture",
        "German Language and Culture",
        "Human Geography",
        "Italian Language and Culture",
        "Japanese Language and Culture",
        "Latin",
        "Macroeconomics",
        "Microeconomics",
        "Music Theory",
        "Physics 1",
        "Physics 2",
        "Physics C: Electricity and Magnetism",
        "Physics C: Mechanics",
        "Precalculus",
        "Psychology",
        "Research",
        "Seminar",
        "Spanish Language and Culture",
        "Spanish Literature and Culture",
        "Statistics",
        "United States Government and Politics",
        "United States History",
        "World History: Modern"
    );

    private FormOptions() {
    }
}
