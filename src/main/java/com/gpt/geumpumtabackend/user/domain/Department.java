package com.gpt.geumpumtabackend.user.domain;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Department {

    architectureEngineering("건축공학전공", "Architecture Engineering"),
    architecture("건축학전공", "Architecture"),
    civilEngineering("토목공학전공", "Civil Engineering"),
    environmentalEngineering("환경공학전공", "Environmental Engineering"),
    mechanicalEngineering("기계공학전공", "Mechanical Engineering"),
    mechanicalSystemsEngineering("기계시스템공학전공", "Mechanical Systems Engineering"),
    smartMobility("스마트모빌리티공학전공", "Smart Mobility Engineering"),
    industrialEngineering("산업공학전공", "Industrial Engineering"),
    appliedMathBigData("수리빅데이터공학전공", "Applied Mathematics and Big Data Engineering"),
    polymerEngineering("고분자공학전공", "Polymer Engineering"),
    materialsEngineering("신소재공학전공", "Materials Engineering"),
    semiconductorSystems("반도체시스템전공", "Semiconductor Systems Engineering"),
    electronicSystems("전자시스템공학전공", "Electronic Systems Engineering"),
    software("소프트웨어전공", "Software Engineering"),
    artificialIntelligence("인공지능공학전공", "Artificial Intelligence Engineering"),
    computerEngineering("컴퓨터공학전공", "Computer Engineering"),
    materialsDesignEngineering("소재디자인공학전공", "Materials Design Engineering"),
    chemicalEngineering("화학공학전공", "Chemical Engineering"),
    chemicalBioMaterials("화학생명공학전공", "Chemical and Biomaterials Engineering"),
    opticalSystems("광시스템공학전공", "Optical Systems Engineering"),
    biomedicalEngineering("바이오메디컬공학전공", "Biomedical Engineering"),
    itConvergence("IT융합학전공", "IT Convergence"),
    liberalMajor("자율전공학부", "Liberal Major"),
    businessAdministration("경영학과", "Business Administration");

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
