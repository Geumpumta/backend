package com.gpt.geumpumtabackend.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Department {

    ARCH_CIV_ENV("건축토목환경공학부", "School of Architecture, Civil and Environmental Engineering"),
    MECHANICAL("기계공학부", "School of Mechanical Engineering"),
    INDUSTRIAL_BIGDATA("산업·빅데이터공학부", "School of Industrial and Big Data Engineering"),
    MATERIALS("재료공학부", "School of Materials Engineering"),
    ELECTRICAL_ELECTRONIC("전자공학부", "School of Electrical and Electronic Engineering"),
    COMPUTER("컴퓨터공학부", "School of Computer Engineering"),
    CHEMICAL_MATERIALS("화학소재공학부", "School of Chemical and Materials Engineering"),
    GENERAL("자율전공학부", "School of General Studies"),
    BUSINESS("경영학과", "Department of Business Administration"),
    LIBERAL("교양학부", "School of Liberal Arts"),
    OPTICAL("광시스템공학과", "Department of Optical Systems Engineering"),
    BIOMEDICAL("바이오메디컬공학과", "Department of Biomedical Engineering"),
    IT_CONVERGENCE("IT융합학과", "Department of IT Convergence");

    private final String koreanName;
    private final String englishName;

    public static Department from(String name) {
        for (Department department : Department.values()) {
            if (department.koreanName.equals(name) || department.englishName.equals(name)) {
                return department;
            }
        }
        return null;
    }
}
