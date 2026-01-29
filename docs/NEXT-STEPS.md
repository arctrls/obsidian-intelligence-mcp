# Obsidian Intelligence MCP Server - 다음 단계 계획

## 현재 상태 (2026-01-29)

### 완료된 작업
- Gradle 프로젝트 구조 (Spring Boot 4.0.2, Kotlin 2.3.0, Java 25, Gradle 8.14)
- Version catalog + Spring AI 2.0.0-M2 BOM
- 전체 패키지 구조 (package-by-feature: document, vault, embedding, search, tagging, related, analysis, shared)
- 141개 단위 테스트 (전부 통과)
- 6개 MCP Tool 클래스 (`@Tool` 어노테이션)
- Docker 설정 (Dockerfile, docker-compose.yml)
- application.yml / application-docker.yml

### 아직 검증하지 않은 것
- Spring Boot 애플리케이션 실제 기동 (`bootRun`)
- MCP 서버 엔드포인트 (`/mcp`) 호출
- PostgreSQL + pgvector 연동
- BGE-M3 ONNX 모델 로딩
- 실제 vault 데이터로 end-to-end 테스트

---

## Phase 7: 애플리케이션 기동 검증

### 7-1. PostgreSQL + pgvector 로컬 실행
```bash
docker compose -f docker/docker-compose.yml up postgres -d
```
- `pgvector/pgvector:pg17` 이미지 pull 확인
- `obsidian_intelligence` DB 생성 확인
- `pg_isready` healthcheck 통과 확인

### 7-2. BGE-M3 ONNX 모델 준비
```bash
pip install optimum onnx onnxruntime sentence-transformers
optimum-cli export onnx --model BAAI/bge-m3 models/
```
- `models/model.onnx` + `models/tokenizer.json` 생성 확인
- Spring AI `TransformersEmbeddingModel`이 로딩하는지 확인
- 모델 파일이 수 GB일 수 있으므로 디스크 공간 확인

**대안**: ONNX 변환이 실패하면 Spring AI의 다른 임베딩 모델 사용 검토
- `all-MiniLM-L6-v2` (384차원, 영어 전용) — 빠르지만 한국어 미지원
- HuggingFace에서 이미 ONNX 변환된 BGE-M3 다운로드 시도

### 7-3. bootRun 기동 테스트
```bash
./gradlew bootRun
```
예상되는 이슈와 해결:
1. **Spring AI 자동 구성 충돌** — `spring-ai-starter-model-transformers`와 `spring-ai-starter-vector-store-pgvector`가 동시에 있을 때 Bean 충돌 가능. `@ConditionalOnProperty`로 조건부 로딩 필요할 수 있음
2. **PgVectorStore 스키마 초기화** — `initialize-schema: true`로 설정했지만 pgvector extension이 먼저 생성되어야 함. Docker 이미지에 이미 포함되어 있지만 확인 필요
3. **ONNX 모델 경로** — `file:models/model.onnx`가 상대 경로로 해석되는지 확인. `file:./models/model.onnx` 또는 절대경로 필요할 수 있음
4. **MCP 서버 transport 설정** — Spring AI MCP의 Streamable HTTP 설정이 `spring.ai.mcp.server.protocol: STREAMABLE`만으로 활성화되는지 확인. WebMVC 스타터가 자동 설정할 수 있지만 추가 property가 필요할 수 있음

### 7-4. MCP 엔드포인트 테스트
```bash
# 기동 후 MCP 엔드포인트 확인
curl http://localhost:8080/mcp
```
- SSE 또는 Streamable HTTP 응답 확인
- MCP tool list 요청/응답 확인

---

## Phase 8: 통합 테스트

### 8-1. Testcontainers 기반 통합 테스트
`src/test/kotlin/.../integration/` 디렉토리에 작성:

```kotlin
@SpringBootTest
@Testcontainers
class McpIntegrationTest {
    @Container
    val postgres = PostgreSQLContainer("pgvector/pgvector:pg17")
    // ...
}
```

검증 항목:
- PgVectorStore 스키마 자동 생성
- 문서 임베딩 → pgvector 저장 → similarity search 왕복
- MCP tool 호출 → JSON 응답 검증

### 8-2. 임베딩 모델 없이 테스트하는 전략
ONNX 모델이 없는 CI 환경에서는:
- `EmbeddingModel`을 mock으로 대체하는 테스트 프로파일
- 또는 Spring AI의 `spring.ai.embedding.transformer.enabled=false`로 비활성화하고 mock Bean 주입

---

## Phase 9: 실제 Vault 연동

### 9-1. Vault 동기화
```bash
# Docker로 전체 실행
docker compose -f docker/docker-compose.yml up
```
- vault mount 확인: `~/Library/Mobile Documents/iCloud~md~obsidian/Documents/jazzbach` → `/vault`
- `DocumentEmbeddingService.syncVault()` 호출하여 전체 인덱싱
- 초기 인덱싱 시간 측정 (vault 크기에 따라 수분~수십분)

### 9-2. MCP 연결
`~/.claude/mcp.json`:
```json
{
  "mcpServers": {
    "obsidian-intelligence": {
      "type": "streamable-http",
      "url": "http://localhost:3001/mcp"
    }
  }
}
```

### 9-3. 실제 도구 호출 확인
Claude Code에서 직접 테스트:
- `search-documents`: "TDD에 대한 문서 찾아줘"
- `tag-document`: 특정 파일 태깅
- `find-related-documents`: 관련 문서 탐색
- `analyze-vault`: vault 통계

---

## Phase 10: 개선 및 안정화

### 10-1. 동기화 트리거
현재 `syncVault()`를 수동 호출해야 함. 개선 방안:
- **시작 시 자동 동기화**: `@EventListener(ApplicationReadyEvent)`
- **주기적 동기화**: `@Scheduled(fixedDelay = 300000)` (5분)
- **MCP tool로 수동 트리거**: `sync-vault` 도구 추가
- **File watcher**: `java.nio.file.WatchService`로 변경 감지 (선택적)

### 10-2. 해시 기반 변경 감지 개선
현재 `getExistingHashes()`가 빈 맵을 반환. 개선 필요:
- PgVectorStore의 metadata에서 fileHash 조회하는 방법 구현
- 또는 별도 `document_hashes` 테이블 생성 (JPA Entity)
- 변경된 문서만 re-embedding하여 동기화 시간 단축

### 10-3. 에러 핸들링 강화
- MCP tool 레벨에서 예외를 잡아 구조화된 에러 응답 반환
- 대용량 파일 처리 시 timeout 설정
- 배치 태깅 시 진행 상황 로깅

### 10-4. 성능 최적화
- ONNX 모델 로딩 시간 최적화 (warm-up)
- 배치 임베딩 크기 튜닝 (`embedding.batchSize`)
- PgVector HNSW 인덱스 파라미터 튜닝 (`m`, `ef_construction`)
- JVM 메모리 설정 (`-Xmx`, ZGC 옵션)

---

## 알려진 기술적 리스크

| 리스크 | 영향 | 대응 방안 |
|---|---|---|
| Spring AI 2.0.0-M2가 milestone 버전 | API 변경 가능 | GA 출시 시 마이그레이션 필요 |
| BGE-M3 ONNX 변환 실패 | 임베딩 불가 | HuggingFace에서 pre-converted 모델 확인, 또는 다른 다국어 모델 사용 |
| Spring Boot 4 + Kotlin 2.3 호환성 | 컴파일/런타임 이슈 | Spring Boot 4 GA release notes 참조 |
| pgvector Docker 이미지 아키텍처 | ARM64 (M시리즈 Mac) 호환성 | `pgvector/pgvector:pg17`이 multi-arch 지원하는지 확인 |
| ONNX Runtime + JVM 25 호환성 | 네이티브 라이브러리 로딩 실패 | JVM 옵션 `--add-opens` 필요할 수 있음 |

---

## 파일 구조 참조

```
obsidian-intelligence-mcp/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── models/                              # .gitignore 대상
├── docs/
│   └── NEXT-STEPS.md                    # 이 문서
└── src/
    ├── main/
    │   ├── kotlin/com/jazzbach/obsidianintelligence/
    │   │   ├── ObsidianIntelligenceApplication.kt
    │   │   ├── document/    (7 files) — 마크다운 파싱
    │   │   ├── vault/       (4 files) — vault 스캔
    │   │   ├── embedding/   (3 files) — pgvector 동기화
    │   │   ├── search/      (6 files) — 시맨틱 검색 + MCP
    │   │   ├── tagging/     (10 files) — 태깅 시스템 + MCP
    │   │   ├── related/     (7 files) — 관련 문서 + MCP
    │   │   ├── analysis/    (4 files) — vault 분석 + MCP
    │   │   └── shared/      (2 files) — 유틸리티
    │   └── resources/
    │       ├── application.yml
    │       └── application-docker.yml
    └── test/kotlin/com/jazzbach/obsidianintelligence/
        ├── document/       (3 tests)
        ├── vault/          (1 test)
        ├── embedding/      (1 test)
        ├── search/         (1 test)
        ├── tagging/        (2 tests)
        ├── related/        (2 tests)
        ├── analysis/       (1 test)
        └── shared/         (2 tests)
        총 141개 테스트 케이스
```
