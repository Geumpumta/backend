package com.gpt.geumpumtabackend.user.domain;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Department {

    ARCHITECTURE_ENGINEERING("건축공학전공", "Architecture Engineering"),
    ARCHITECTURE("건축학전공", "Architecture"),
    CIVIL_ENGINEERING("토목공학전공", "Civil Engineering"),
    ENVIRONMENTAL_ENGINEERING("환경공학전공", "Environmental Engineering"),
    MECHANICAL_ENGINEERING("기계공학전공", "Mechanical Engineering"),
    MECHANICAL_SYSTEMS_ENGINEERING("기계시스템공학전공", "Mechanical Systems Engineering"),
    SMART_MOBILITY("스마트모빌리티전공", "Smart Mobility Engineering"),
    INDUSTRIAL_ENGINEERING("산업공학전공", "Industrial Engineering"),
    APPLIED_MATH_BIGDATA("수리빅데이터전공", "Applied Mathematics and Big Data Engineering"),
    POLYMER_ENGINEERING("고분자공학전공", "Polymer Engineering"),
    MATERIALS_ENGINEERING("신소재공학전공", "Materials Engineering"),
    SEMICONDUCTOR_SYSTEMS("반도체시스템전공", "Semiconductor Systems Engineering"),
    ELECTRONIC_SYSTEMS("전자시스템전공", "Electronic Systems Engineering"),
    SOFTWARE("소프트웨어전공", "Software Engineering"),
    ARTIFICIAL_INTELLIGENCE("인공지능공학전공", "Artificial Intelligence Engineering"),
    COMPUTER_ENGINEERING("컴퓨터공학전공", "Computer Engineering"),
    MATERIALS_DESIGN_ENGINEERING("소재디자인공학전공", "Materials Design Engineering"),
    CHEMICAL_ENGINEERING("화학공학전공", "Chemical Engineering"),
    CHEMICAL_BIO_MATERIALS("화학생명소재전공", "Chemical and Biomaterials Engineering"),
    OPTICAL_SYSTEMS("광시스템공학과", "Optical Systems Engineering"),
    BIOMEDICAL_ENGINEERING("바이오메디컬공학과", "Biomedical Engineering"),
    IT_CONVERGENCE("IT융합학과", "IT Convergence"),
    LIBERAL_MAJOR("자율전공학부", "Liberal Major"),
    BUSINESS_ADMINISTRATION("경영학과", "Business Administration");


    private final String koreanName;
    private final String englishName;

    public static Department from(String name) {
        for (Department department : Department.values()) {
            if (department.koreanName.equals(name) || department.englishName.equals(name)) {
                return department;
            }
        }
        throw new BusinessException(ExceptionType.DEPARTMENT_NOT_FOUND);
    }
}
