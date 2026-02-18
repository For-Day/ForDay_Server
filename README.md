# ForDay_Server: 당신의 취미, 66일을 함께 채워나가요.


## 📜 프로젝트 문서 ➡️ [ForDay Server Wiki](https://github.com/Central-MakeUs/ForDay_Server/wiki)

<br>

## 🛠 기술 스택

- **Backend**  
  ![Spring Boot](https://img.shields.io/badge/SPRINGBOOT-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
  ![Java](https://img.shields.io/badge/JAVA-007396?style=for-the-badge&logo=openjdk&logoColor=white)
  ![JPA](https://img.shields.io/badge/JPA(HIBERNATE)-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
  ![QueryDSL](https://img.shields.io/badge/QUERYDSL-005571?style=for-the-badge)

- **Infra**  
  ![EC2](https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
  ![S3](https://img.shields.io/badge/S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
  ![RDS](https://img.shields.io/badge/RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)
  ![Lambda](https://img.shields.io/badge/Lambda-FF9900?style=for-the-badge&logo=awslambda&logoColor=white)
  ![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)

- **AI**  
  ![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
  ![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)

- **DB**  
  ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
  ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

<br>

## 🌍 아키텍처 구조
![INFRA](./assets/infra.png)

<br>

## 📝 ERD 설계
![ERD](./assets/erd.png)

<br>

## ✅ 이슈 · 커밋 · 브랜치 전략

### ✔️ 이슈, 커밋, 브랜치명 규칙

- **이슈: `[prefix] 작업내용`**
    - 예: **[feat] 카카오 로그인 구현**

- **브랜치: `[prefix]/#이슈번호-설명`**
    - 예: **feat/#12-kakao-oauth-login**

- **커밋: `[ #이슈번호 ] prefix: 작업내용`**
    - 예: **[#51] feat: 카카오 로그인 에러 해결**

<br>

| **prefix** | **definition** |
| --- | --- |
| feat | 새로운 기능 추가 |
| fix | 기능 수정 |
| chore | 설정 / 환경 구성 |
| bug | 오류 수정 |
| hotfix | 긴급 수정 |
| refactor | 코드 리팩토링 |
| docs | 문서 작업 |
| test | 테스트 코드 작성 |
| setting | 세팅 관련 코드 |
| deploy | 배포 관련 코드 |

<br>

### ✔️ 깃 브랜치 전략

- **main** : 운영 환경에 배포되는 최종 코드
- **release** : 개발 환경에 배포되는 코드
- **dev** : 기능 통합 브랜치
- **feat/*** : 단일 기능 개발 브랜치

