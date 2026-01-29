package com.jazzbach.obsidianintelligence.expansion

object KoreanSynonymDictionary {

    private val synonymGroups: List<Set<String>> = listOf(
        // Programming Languages
        setOf("코틀린", "kotlin", "kt"),
        setOf("자바", "java", "jdk", "jvm"),
        setOf("파이썬", "python", "py"),
        setOf("자바스크립트", "javascript", "js", "ecmascript"),
        setOf("타입스크립트", "typescript", "ts"),
        setOf("고", "golang", "go"),
        setOf("러스트", "rust"),
        setOf("스위프트", "swift"),

        // Frameworks
        setOf("스프링", "spring", "스프링부트", "spring-boot", "springboot"),
        setOf("리액트", "react", "reactjs"),
        setOf("뷰", "vue", "vuejs"),
        setOf("앵귤러", "angular", "angularjs"),
        setOf("넥스트", "next", "nextjs", "next.js"),
        setOf("장고", "django"),
        setOf("플라스크", "flask"),
        setOf("익스프레스", "express", "expressjs"),

        // Architecture & Design
        setOf("아키텍처", "architecture", "구조", "설계"),
        setOf("디자인패턴", "design-pattern", "설계패턴", "패턴"),
        setOf("마이크로서비스", "microservice", "msa"),
        setOf("모놀리스", "monolith", "모놀리식"),
        setOf("헥사고날", "hexagonal", "포트앤어댑터", "ports-and-adapters"),
        setOf("클린아키텍처", "clean-architecture", "클린"),
        setOf("도메인주도설계", "ddd", "domain-driven-design"),
        setOf("이벤트소싱", "event-sourcing", "이벤트"),
        setOf("cqrs", "명령조회분리"),

        // Testing
        setOf("테스트", "test", "testing", "테스팅"),
        setOf("단위테스트", "unit-test", "유닛테스트"),
        setOf("통합테스트", "integration-test", "인티그레이션"),
        setOf("tdd", "테스트주도개발", "test-driven-development"),
        setOf("bdd", "행동주도개발", "behavior-driven-development"),
        setOf("목", "mock", "모킹", "mocking"),

        // DevOps & Infrastructure
        setOf("도커", "docker", "컨테이너", "container"),
        setOf("쿠버네티스", "kubernetes", "k8s"),
        setOf("ci", "cd", "cicd", "ci/cd", "지속적통합", "지속적배포"),
        setOf("깃", "git", "깃허브", "github"),
        setOf("젠킨스", "jenkins"),
        setOf("aws", "아마존", "amazon"),
        setOf("클라우드", "cloud"),

        // Database
        setOf("데이터베이스", "database", "db", "디비"),
        setOf("포스트그레스", "postgresql", "postgres"),
        setOf("몽고", "mongodb", "mongo"),
        setOf("레디스", "redis", "캐시", "cache"),
        setOf("sql", "쿼리", "query"),
        setOf("jpa", "하이버네이트", "hibernate", "orm"),

        // Concepts
        setOf("리팩토링", "refactoring", "리팩터링"),
        setOf("성능", "performance", "최적화", "optimization"),
        setOf("보안", "security", "시큐리티"),
        setOf("인증", "authentication", "auth", "로그인", "login"),
        setOf("인가", "authorization", "권한"),
        setOf("api", "에이피아이", "인터페이스", "interface"),
        setOf("rest", "restful", "레스트"),
        setOf("grpc", "지알피씨"),
        setOf("메시지큐", "message-queue", "mq", "카프카", "kafka", "래빗", "rabbitmq"),

        // Software Engineering
        setOf("객체지향", "oop", "object-oriented"),
        setOf("함수형", "functional", "fp"),
        setOf("동시성", "concurrency", "병렬", "parallel"),
        setOf("비동기", "async", "asynchronous", "코루틴", "coroutine"),
        setOf("의존성주입", "dependency-injection", "di"),
        setOf("솔리드", "solid", "solid원칙"),
        setOf("클래스", "class"),
        setOf("상속", "inheritance"),
        setOf("다형성", "polymorphism"),
        setOf("캡슐화", "encapsulation"),

        // Data
        setOf("알고리즘", "algorithm"),
        setOf("자료구조", "data-structure"),
        setOf("배열", "array", "리스트", "list"),
        setOf("해시", "hash", "해시맵", "hashmap", "맵", "map"),
        setOf("트리", "tree", "이진트리"),
        setOf("그래프", "graph"),

        // Web
        setOf("프론트엔드", "frontend", "front-end", "프런트"),
        setOf("백엔드", "backend", "back-end", "서버", "server"),
        setOf("웹", "web"),
        setOf("모바일", "mobile", "앱", "app"),

        // Methodology
        setOf("애자일", "agile"),
        setOf("스크럼", "scrum"),
        setOf("칸반", "kanban"),
        setOf("페어프로그래밍", "pair-programming", "페어"),
        setOf("코드리뷰", "code-review", "리뷰"),

        // Documentation
        setOf("문서", "document", "documentation", "문서화"),
        setOf("노트", "note", "메모", "memo"),
        setOf("마크다운", "markdown", "md")
    )

    private val lookupMap: Map<String, Set<String>> = buildMap {
        for (group in synonymGroups) {
            for (term in group) {
                put(term.lowercase(), group)
            }
        }
    }

    fun findSynonyms(term: String): Set<String> {
        val normalized = term.lowercase().trim()
        val exactMatch = lookupMap[normalized]
        if (exactMatch != null) {
            return exactMatch - normalized
        }

        val partialMatches = mutableSetOf<String>()
        for ((key, group) in lookupMap) {
            if (key.contains(normalized) || normalized.contains(key)) {
                partialMatches.addAll(group)
            }
        }
        partialMatches.remove(normalized)
        return partialMatches
    }

    fun hasSynonym(term: String): Boolean {
        val normalized = term.lowercase().trim()
        if (lookupMap.containsKey(normalized)) return true
        return lookupMap.keys.any { it.contains(normalized) || normalized.contains(it) }
    }
}
