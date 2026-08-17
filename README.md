# PDF Inspector Java

Firecrawl의 [`pdf-inspector`](https://github.com/firecrawl/pdf-inspector) Rust 프로젝트를 Java/PDFBox 생태계로 옮기는 Gradle 기반 구현입니다. 원본의 MIT 라이선스와 attribution은 [LICENSE](LICENSE)에 유지합니다.

## 런타임 정책

- 빌드·실행은 **JDK 17 이상만 허용**합니다. Gradle configuration-time 검사와 `--release 17`이 이를 보장합니다.
- 라이브러리 소스와 산출물은 `--release 17`로 컴파일합니다. Java 8~16은 지원하지 않으며, 라이브러리 API와 CLI도 시작 시 이를 명시적으로 거부합니다.
- PDFBox 3.0.8을 사용합니다. PDFBox 자체의 하한은 Java 8이지만, 이 프로젝트의 하한은 JDK 17입니다.

## 빠른 실행

```powershell
cd C:\Users\ipeac\IdeaProjects\pdf-inspector-java
.\gradlew.bat build

# PDF → Markdown
java -jar build\libs\pdf-inspector-java-0.1.0-SNAPSHOT-all.jar src\test\resources\fixtures\thermo-freon12.pdf --raw

# PDF 유형 감지
.\gradlew.bat detectPdf -PcliArgs="src\test\resources\fixtures\thermo-freon12.pdf --analyze --json"

# Windows wrapper commands (after build)
.\bin\pdf2md.bat src\test\resources\fixtures\thermo-freon12.pdf --raw
.\bin\detect-pdf.bat src\test\resources\fixtures\thermo-freon12.pdf --analyze --json
```

## 성능 회귀 기준

`performanceTest`는 고정 fixture마다 10회 워밍업 뒤 20회의 처리 시간을 측정해 median/p95와 원시 표본을 기록합니다. 기준선과 같은 JDK·OS 환경에서 median이 10%를 초과해 느려지면 실패합니다.

```powershell
# 최신 측정값 확인 및, 검토 후 로컬 기준선 갱신
.\gradlew.bat performanceTest
.\gradlew.bat performanceTest -PupdatePerformanceBaseline
```

## 현재 제공 범위

- PDFBox 문서 1회 로드·텍스트 순회 후 감지와 추출이 같은 `TextItem` 집합을 공유
- `TEXT_BASED`, `SCANNED`, `IMAGE_BASED`, `MIXED` 분류와 페이지별 OCR 사유
- 문자별 좌표·폰트 정보를 가진 `TextItem`, 라인 조립, 기본 제목/목록/문단 Markdown
- Rust CLI와 같은 이름·의도의 `pdf2md`, `detect-pdf` 기능 (`--json`, `--items-json`, `--analyze`, `--raw`, `--compact`, `--pages`, `--select-pages`, `--password`)

아직 완전한 표 격자 변환, 다단 신문형 순서 최적화, 구조 트리 역할 반영을 동등하게 구현한 상태는 아닙니다. 해당 확장 순서와 대응 위치는 [PORTING_GUIDE.md](PORTING_GUIDE.md)에 명시했습니다.

## 소스 구성

```text
src/main/java/dev/pdfinspector/
  PdfInspector.java       public API and one-load orchestration
  detector/               page sampling and OCR-routing classification
  extractor/              PDFBox TextPosition to positioned text items
  markdown/               line assembly and Markdown rendering
  tables/                 explicit future table-detection seam
  model/                  stable public value objects and options
  cli/                    pdf2md and detect-pdf compatible commands
```
