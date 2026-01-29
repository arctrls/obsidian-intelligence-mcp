# vault-intelligence → obsidian-intelligence-mcp 이식 현황

## 원본 프로젝트

- **저장소**: `/Users/jake/projects/vault-intelligence`
- **언어**: Python 3.11+
- **핵심 기술**: BGE-M3 (FlagEmbedding), FAISS, SQLite, BM25
- **인터페이스**: CLI (argparse)

## MCP 프로젝트

- **저장소**: `/Users/jake/projects/obsidian-intelligence-mcp`
- **언어**: Kotlin 2.3.0
- **핵심 기술**: Spring Boot 4.0.2, Spring AI 2.0.0-M2, pgvector, ONNX Transformer
- **인터페이스**: MCP Server (Streamable HTTP)

---

## 아키텍처 변환

| 항목 | vault-intelligence (Python) | obsidian-intelligence-mcp (Kotlin) |
|---|---|---|
| 임베딩 모델 | FlagEmbedding (BGE-M3 네이티브) | Spring AI ONNX Transformer |
| 벡터 저장소 | FAISS (인메모리) + SQLite 캐시 | PostgreSQL + pgvector |
| 임베딩 차원 | 1024 (Dense) + Sparse + ColBERT | 1024 (Dense만) |
| 설정 관리 | YAML (`config/settings.yaml`) | Spring `application.yml` + `@ConfigurationProperties` |
| 의존성 관리 | `requirements.txt` | Gradle Version Catalog |
| 컨테이너 | 없음 | Docker + docker-compose |
| 인터페이스 | CLI (argparse) | MCP Server (`@Tool` 어노테이션) |
| 테스트 | 없음 | JUnit 5 + MockK (170개) |

---

## 기능별 이식 현황

### 이식 완료

| 기능 | 원본 소스 | MCP 패키지 | MCP Tool | 비고 |
|---|---|---|---|---|
| Vault 스캔 | `vault_processor.py` | `vault/` | — | 파일 탐색, 확장자 필터, 디렉토리 제외 |
| 마크다운 파싱 | `vault_processor.py` | `document/` | — | 제목 추출, 태그 수집, 단어 수 계산 |
| Frontmatter 파싱 | `vault_processor.py` | `document/YamlFrontmatterParser` | — | YAML 파싱, 유연한 태그 형식 지원 |
| Content Cleaning | `vault_processor.py` 내장 | `document/ContentCleaner` | — | 마크다운 구문 제거, 위키링크 처리 |
| 임베딩 동기화 | `embedding_cache.py` | `embedding/DocumentEmbeddingService` | — | 배치 처리, 해시 기반 변경 감지 (미완성) |
| 시맨틱 검색 (Dense) | `advanced_search.py` dense 부분 | `search/SemanticSearchService` | `search-documents` | 태그 필터, 경로 제외, 스니펫 생성 |
| 시맨틱 태깅 | `semantic_tagger.py` | `tagging/SemanticTaggingService` | `tag-document` | 유사 문서 기반 + 패턴 + 토픽 태그 |
| 태그 규칙 엔진 | `tag_rule_engine.py` | `tagging/DefaultTagRuleEngine` | — | 정규화, 검증, 카테고리 분류, 계층 구조 |
| 배치 태깅 | `semantic_tagger.py` 배치 | `tagging/BatchTaggingService` | `batch-tag-folder` | 폴더 단위 태깅 |
| 관련 문서 탐색 | `related_docs_finder.py` | `related/RelatedDocsService` | `find-related-documents` | 유사도 기반, 자기 자신 제외 |
| 관련 문서 섹션 업데이트 | `related_docs_finder.py` | `related/RelatedSectionUpdater` | `update-related-section` | 위키링크 형식으로 마크다운 삽입 |
| Vault 분석 | CLI stats 기능 | `analysis/VaultAnalysisService` | `analyze-vault` | 파일 수, 단어 수, 태그 빈도, 카테고리 분포 |
| Query Expansion | `query_expansion.py` | `expansion/QueryExpansionService` | — | 한국어 동의어 사전 + 규칙 기반 HyDE (LLM 미사용) |
| 키워드 검색 | `advanced_search.py` BM25 부분 | `search/KeywordSearchService` | `search-documents` (searchType=KEYWORD) | 제목/태그/본문 가중치 기반 키워드 점수 |
| 하이브리드 검색 (RRF) | `advanced_search.py` | `search/SemanticSearchService` | `search-documents` (searchType=HYBRID) | Dense * 0.7 + Keyword * 0.3 융합 |
| 중복 문서 탐지 | `duplicate_detector.py` | `duplicate/DuplicateDetectionService` | `detect-duplicates` | pgvector 유사도 기반 그룹핑, master 선정 |
| 토픽 수집 | `topic_collector.py` | `topic/TopicCollectionService` | `collect-topic` | 검색 + 태그 기반 그룹핑, 통계, 관련 토픽 추천 |

### 미이식 기능

| 기능 | 원본 소스 | 코드량 | 설명 | MCP 이식 난이도 |
|---|---|---|---|---|
| **ColBERT 검색** | `colbert_search.py` | 496줄 | 토큰 레벨 정밀 매칭 | 높음 — ONNX에서 ColBERT 임베딩 미지원 |
| **Reranker** | `reranker.py` | 387줄 | bge-reranker-v2-m3 교차 인코더 | 높음 — JVM에서 cross-encoder 실행 필요 |
| **문서 클러스터링** | `content_clusterer.py` | 529줄 | K-means, DBSCAN, Agglomerative | 중 — JVM ML 라이브러리 (Smile 등) 필요 |
| **문서 요약** | `document_summarizer.py` | 432줄 | 다중 요약 스타일, LLM 연동 | 낮음 — Spring AI ChatClient로 구현 가능 |
| **지식 그래프** | `knowledge_graph.py` | 922줄 | 관계 분석, 커뮤니티 탐지, 중심성 | 높음 — 그래프 라이브러리 (JGraphT 등) 필요 |
| **MOC 생성** | `moc_generator.py` | 892줄 | 계층적 Map of Content 자동 생성 | 중 — 클러스터링 결과 기반 마크다운 생성 |
| **토픽 분석** | `topic_analyzer.py` | 1,005줄 | 다중 알고리즘, 키워드 추출, 시각화 | 높음 — ML 라이브러리 + 시각화 필요 |
| **학습 리뷰** | `learning_reviewer.py` | 593줄 | 활동 통계, 진도 추적, 주기별 리뷰 | 중 — 시계열 분석 + 통계 집계 |

---

## 설정 비교

### Vault 설정

| 항목 | vault-intelligence | MCP |
|---|---|---|
| vault 경로 | `/Users/jake/obsidian` | `/vault` (Docker mount) |
| 제외 디렉토리 | .obsidian, .git, .claude, WIP, ATTACHMENTS, Template 등 | .obsidian, .trash, .git, ATTACHMENTS |
| 제외 파일 | *.log, *.tmp, *.backup, .gitkeep, README.md 등 | 없음 (디렉토리만 제외) |
| 파일 확장자 | .md, .markdown | .md, .markdown |
| 최대 파일 크기 | 10MB | 없음 |

### 검색 설정

| 항목 | vault-intelligence | MCP |
|---|---|---|
| 기본 top_k | 10 | 10 |
| 유사도 임계값 | 0.3 | 0.3 |
| 하이브리드 가중치 | semantic 0.7, text 0.3 | dense 0.7, keyword 0.3 |
| Reranker | bge-reranker-v2-m3 | — |

### 태깅 설정

| 항목 | vault-intelligence | MCP |
|---|---|---|
| 문서당 최대 태그 | 10 | 10 |
| 최소 유사도 | 0.3 | 0.3 |
| 토픽 태그 최대 | — | 4 |
| 문서유형 태그 최대 | — | 1 |
| 출처 태그 최대 | — | 1 |
| 패턴 태그 최대 | — | 3 |
| 프레임워크 태그 최대 | — | 2 |

> MCP 버전이 카테고리별 태그 제한을 더 세밀하게 관리함

---

## MCP Tool 매핑

| MCP Tool | 원본 CLI 명령어 | 상태 |
|---|---|---|
| `search-documents` | `python -m src search <query>` | 완료 (Dense만) |
| `tag-document` | (내장 기능) | 완료 |
| `batch-tag-folder` | (내장 기능) | 완료 |
| `find-related-documents` | (내장 기능) | 완료 |
| `update-related-section` | (내장 기능) | 완료 |
| `analyze-vault` | `python -m src stats` | 완료 |
| `detect-duplicates` | `python -m src duplicates` | 완료 |
| `collect-topic` | `python -m src collect` | 완료 |
| — | `python -m src index` | MCP에서 `sync-vault` 도구 필요 |
| — | `python -m src analyze` | 미이식 (토픽 분석) |
| — | `python -m src graph` | 미이식 (지식 그래프) |

---

## 기술 스택 비교

| 카테고리 | vault-intelligence | obsidian-intelligence-mcp |
|---|---|---|
| 언어 | Python 3.11+ | Kotlin 2.3.0 |
| 프레임워크 | — (순수 Python) | Spring Boot 4.0.2 |
| AI 프레임워크 | FlagEmbedding, sentence-transformers | Spring AI 2.0.0-M2 |
| 임베딩 모델 | BGE-M3 (네이티브 PyTorch) | BGE-M3 (ONNX 변환 필요) |
| Reranker 모델 | bge-reranker-v2-m3 | — |
| 벡터 검색 | FAISS (인메모리) | pgvector (PostgreSQL) |
| 키워드 검색 | rank-bm25 | KeywordSearchService (가중치 기반) |
| DB | SQLite (SQLAlchemy) | PostgreSQL (Spring Data JPA) |
| ML 라이브러리 | scikit-learn, numpy, pandas | — |
| 그래프 분석 | networkx | — |
| 시각화 | matplotlib | — |
| 런타임 | CPython + MPS (M1 가속) | JVM 25 (ZGC) |
| 빌드 | pip | Gradle 8.14 |
| 컨테이너 | — | Docker + docker-compose |
| 테스트 | — | JUnit 5, MockK (170개) |

---

## 이식 시 아키텍처 결정사항

### 1. 임베딩 모델
- **원본**: FlagEmbedding으로 BGE-M3를 네이티브 로딩 (Dense + Sparse + ColBERT 동시 지원)
- **MCP**: Spring AI ONNX Transformer로 Dense 임베딩만 지원
- **영향**: Sparse/ColBERT 검색 이식 불가 → Dense + pgvector 전문 검색(tsvector) 조합으로 대체 검토

### 2. 벡터 저장소
- **원본**: FAISS 인메모리 → 프로세스 종료 시 SQLite에 캐시
- **MCP**: pgvector → 영구 저장, HNSW 인덱스, SQL 쿼리와 통합 가능
- **이점**: 서버 재시작 시 재인덱싱 불필요, 메타데이터 필터링 SQL로 처리

### 3. 검색 파이프라인
- **원본**: Dense → Sparse → ColBERT → RRF 융합 → Reranker (5단계)
- **MCP**: Dense → 결과 반환 (1단계)
- **대안**: pgvector 유사도 + PostgreSQL tsvector 전문 검색 → RRF → Spring AI ChatModel 기반 reranking

### 4. 분석 기능
- **원본**: scikit-learn (클러스터링), networkx (그래프), matplotlib (시각화)
- **MCP**: JVM 생태계에서 대응 라이브러리 필요
- **후보**: Smile (ML), JGraphT (그래프), 시각화는 MCP 특성상 불필요 (텍스트 응답)
