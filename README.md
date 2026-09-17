# 소설 검색 프로그램 (NovelList)

네이버 시리즈 / 카카오페이지 / 노벨피아 3개 플랫폼에서 제목·작가·태그(장르) 조건으로 소설을 검색해서 점수순으로 정리해주는 데스크톱 앱.

## 고지 사항

이 프로그램은 각 플랫폼이 공개적으로 제공하는 페이지(제목/평점/태그/작가/줄거리 등)를 개인적인 검색·열람 목적으로 수집하는 개인 학습용 프로젝트입니다. 로그인이 필요한 콘텐츠는 우회하지 않고 건너뛰도록 만들어져 있습니다.

각 플랫폼의 이용약관은 자동화된 수단을 통한 정보 수집을 제한할 수 있으며, 수집한 데이터를 재배포·상업적으로 이용하는 것은 저작권 문제가 될 수 있습니다. 이 저장소는 코드만 공개하며 수집 결과물은 포함하지 않습니다(`.gitignore`로 제외). 이 프로그램을 실행해서 얻는 결과의 이용에 대한 책임은 사용자 본인에게 있으며, 각 플랫폼의 이용약관을 확인하고 개인적인 용도로만 사용하시기 바랍니다.

## 주요 기능

- 플랫폼 3개(Series/KakaoPage/NovelPia) 동시 검색, 각각 결과창 따로 표시
- 제목 / 작가 / 태그(직접 입력) / 장르(체크박스) 조건 검색
- 태그 합집합(∪, 하나라도 겹치면 통과) · 교집합(∩, 전부 있어야 통과) 연산 선택
- 검색 진행상황을 별도 창(`ProgressWindow`)에서 실시간 표시
- 결과를 점수순 정렬 후 `결과/` 폴더에 텍스트(`.txt`)와 JSON(`.json`) 두 형식으로 저장
- 결과창은 카드형 UI(FlatLaf 기반)로 제목/평점/작가/태그/설명 확인
- 카카오페이지의 로그인 필요(성인/청불) 콘텐츠를 빠르게 감지해서 건너뜀 (불필요한 대기시간 절약)

## 실행 방법

### 1. 소스에서 바로 실행

```bash
./gradlew run
```

### 2. 빌드된 jar로 실행

```bash
./gradlew shadowJar
java -jar build/libs/NovelList-all.jar
```

### 3. exe로 실행 (Windows, Java 설치 불필요)

```bash
./gradlew buildExeDist
```

번들된 경량 JRE를 함께 생성하므로 실행 PC에 Java가 없어도 동작함. 결과물은 `build/launch4j/build/libs/NovelList.exe`에 생기며, 같은 폴더의 `jre/`와 상위 `lib/` 폴더의 jar들이 함께 있어야 정상 동작함 — exe 하나만 복사하면 안 되고, `build/launch4j/` 하위 전체를 배포해야 함.

## 요구사항

- JDK 21
- Chrome 브라우저 (카카오페이지 크롤링에 Selenium 사용, 나머지는 브라우저 없이 정적 HTML 파싱)

## 테스트

```bash
./gradlew test
```

## 기술 스택

- Swing + FlatLaf — GUI
- jsoup — 네이버/노벨피아 정적 HTML 파싱 (브라우저 구동 없음)
- Selenium + ChromeDriver — 카카오페이지 (JS 렌더링 SPA라 불가피)
- SLF4J + Logback — 로깅 (콘솔 + `logs/novellist.log`)
- JUnit5 — 테스트

## 프로젝트 구조

- `Main.java` — 진입점. 룩앤필/로깅 초기화, ChromeDriver 세팅, 검색 실행 및 결과 저장
- `UI.java` — 검색조건 입력창(`First`) / 결과 표시창(`End`)
- `ProgressWindow.java` — 크롤링 진행상황 표시창
- `SearchQuery.java` — 검색 조건 DTO
- `Book.java` — 크롤링 결과 DTO
- `Execution.java` — 크롤링 엔진 (플랫폼별 검색 + 책 상세 크롤링)
- `SelectorConfig.java` / `selectors.properties` — CSS 셀렉터 외부 설정 (사이트 개편 시 재빌드 없이 수정 가능)

## 알려진 한계

- 네이버/노벨피아는 화면에 보이는 페이지네이션 링크까지만 수집 (장르당 수천~수만 건 중 일부만)
- 카카오페이지 무한스크롤은 `scrollmax=10`으로 고정되어 있어 대량 장르는 일부만 수집됨
- 3개 플랫폼 모두 공식 API가 없어 스크래핑 방식 — 사이트 구조가 바뀌면 셀렉터가 깨질 수 있음 (그 경우 재빌드 없이 `selectors.properties`만 수정하면 됨)
