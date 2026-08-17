# Rust → Java 이식 가이드

이 프로젝트의 기준 동작은 상위 `pdf-inspector` Rust 구현입니다. Java 코드는 PDFBox가 이미 제공하는 파싱·폰트 디코딩·텍스트 위치 추적을 우선 사용하고, PDFBox가 노출하지 않는 의미 복구만 별도 알고리즘으로 둡니다.

## 경계와 책임

| Rust 영역 | Java 영역 | 현재 상태 |
| --- | --- | --- |
| `lib.rs` | `PdfInspector` + `model` | 구현 |
| `detector.rs` | `detector/PdfTypeDetector` | 구현: 샘플링, 텍스트/이미지 기반 OCR 라우팅 |
| `extractor/content_stream.rs`, `fonts.rs` | `extractor/PdfBoxContentStreamExtractor` | 구현: PDFBox content-stream/TextPosition 우선, ToUnicode·TrueType fallback |
| `extractor/layout.rs`, `reading_order.rs` | `markdown/TextLineAssembler` | 기본 읽기 순서 구현, 고급 다단은 다음 단계 |
| `markdown/*` | `markdown/MarkdownRenderer` | 제목·목록·문단·압축 프로필 구현 |
| `tables/*` | `tables/TableDetector` | 감지 확장 지점만 구현 |
| `tounicode.rs` | `extractor/ToUnicodeFallbackDecoder` | PDFBox 우선, `bfchar`/`bfrange`·TrueType cmap fallback 구현 |

## 구현 원칙

1. `PDDocument`는 `PdfInspector`에서 한 번만 열고 같은 인스턴스를 detector와 extractor에 넘긴다. PDFBox 3의 지연 파싱 특성상 닫힌 뒤에 page/resource를 건드리지 않는다.
2. PDFBox의 content-stream 기반 `PDFTextStripper` 확장을 한 곳에서만 감싼다. 애플리케이션 곳곳에서 PDFBox protected callback을 직접 다루지 않는다.
3. 공개 모델은 immutable value object로 유지한다. PDFBox의 mutable 객체는 내부 구현에만 둔다.
4. Java 17을 기준으로 구현한다. record, sealed class 등 언어 기능은 공개 API의 가독성과 호환성에 이점이 명확할 때만 도입한다.
5. 암호는 옵션의 `toString()`에 노출하지 않는다.

## 다음 구현 순서

1. 다단 valley histogram, spanning line pre-mask, newspaper/tabular reading-order 분기 이식
2. `tables`에 사각형/선/텍스트 정렬 기반의 세 전략과 Markdown grid formatter 이식
3. structure tree(H1-H6, P, L, Code, BlockQuote) 소비와 markdown role 우선순위 추가
4. `src/test/resources/fixtures`에 JSON/Markdown 스냅샷 비교를 추가하고 `pdf-evals` 의미 점수 비교를 연결

## 검증 규칙

모든 변경은 아래를 통과해야 합니다.

```powershell
.\gradlew.bat build
.\gradlew.bat performanceTest
```

성능 기준선은 `benchmarks/performance-baseline.json`에 같은 JDK·OS 조합으로만 비교된다. 갱신은 측정 결과를 검토한 뒤 `-PupdatePerformanceBaseline`을 명시한 경우에만 수행한다.

기능 동등성 판단은 단순 문자열 diff보다 문단·읽기 순서·표 구조를 보는 의미 비교를 우선합니다. Rust 쪽의 `pdf-evals` semantic score와 같은 기준을 Java 포트에도 연결할 계획입니다.
