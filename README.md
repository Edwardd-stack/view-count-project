# 멀티스레드 환경에서의 캐시–DB 조회수 불일치 실험

## 1. 실험 목적

이 과제의 목적은 다음과 같다.

1. 멀티스레드 환경에서 여러 스레드가 동시에 “게시글 조회 API”를 호출할 때,
   캐시와 DB 사이에 **타이밍 이슈**와 **race condition**이 어떻게 발생하는지 직접 확인한다.
2. 조회수 증가 과정에서 **조회수 손실(lost update)** 및  
   **캐시–DB 불일치**가 실제로 발생하는 예시를 로그로 남긴다.
3. 이후 전체 연산에 **락(lock)** 을 적용하여 문제를 해결하고,  
   해결 전·후 결과를 비교한다.

---

## 2. 실험 환경

- 언어: Java (OpenJDK 21, Temurin)
- 실행 방식: 단일 Java 콘솔 애플리케이션
- DB / 캐시:
  - 실제 RDBMS, Redis를 사용하지 않고 **in-memory 구조**로 구현
  - `FakeDatabase` : 전역 변수 하나를 가진 가상의 DB
  - `FakeCache` : 별도의 전역 변수를 가진 가상의 캐시

---

## 3. 조회 API 동작 구조

조회 API는 다음 순서대로 동작하도록 설계되었다.

1. 캐시에서 조회수 읽기
2. 캐시 값이 없으면 DB에서 읽어 캐시에 저장
3. 조회수 +1 증가
4. DB에 저장
5. 캐시에 저장
6. 최종 조회수 반환

기본(문제 있는) 코드는 다음과 같다.

```java
public int increaseViewCount() {
    Integer cached = cache.get();

    if (cached == null) {
        int dbValue = db.getViewCount();
        cache.set(dbValue);
        cached = dbValue;
    }

    int newValue = cached + 1;

    db.saveViewCount(newValue);
    cache.set(newValue);

    return newValue;
}
```

---

## 4. 멀티스레드 테스트 시나리오

- 스레드 수: `20`
- 각 스레드 반복 횟수: `500`
- 기대 조회수: `20 × 500 = 10,000`
- 테스트 방식: ExecutorService + CountDownLatch 사용

---

## 5. 기본 구조 테스트 결과 (LOCK 없음, 문제 재현 성공)

### Naive Version (LOCK YO'Q)

```
Expected total calls: 10000
DB viewCount        : 510
Cache viewCount     : 510
Time(ms)            : 776
```

### 분석

- 10,000번 호출했지만 DB/Cache 값이 **510**에 불과
- 즉, **9,490번 증가 연산이 사라짐 → lost update 전형적인 사례**
- 테스트마다 결과가 불안정하며, DB/Cache 값이 서로 다르게 나올 때도 있음
- race condition, 캐시 미스 동시 발생으로 인한 덮어쓰기 등 문제가 자연스럽게 발생

---

## 6. 해결 전략 (전체 연산 Lock)

전체 조회 로직을 `synchronized(lock)` 으로 감싸 모든 스레드가  
**한 번에 하나씩**만 증가 연산을 수행하도록 개선하였다.

```java
private final Object lock = new Object();

public int increaseViewCountWithLock() {
    synchronized (lock) {
        Integer cached = cache.get();

        if (cached == null) {
            int dbValue = db.getViewCount();
            cache.set(dbValue);
            cached = dbValue;
        }

        int newValue = cached + 1;

        db.saveViewCount(newValue);
        cache.set(newValue);

        return newValue;
    }
}
```

---

## 7. 개선 후 테스트 결과 (LOCK BOR)

### Locked Version (정상 동작)

```
Expected total calls: 50000
DB viewCount        : 50000
Cache viewCount     : 50000
Time(ms)            : 77741
```

### 분석

- DB = Cache = Expected → **정합성 100% 보장**
- lost update 전혀 발생하지 않음
- 실행 시간은 증가했지만 이는 락 경쟁으로 자연스러운 현상

---

## 8. 비교 요약

| 구분           | LOCK 없음 | LOCK 있음 |
| -------------- | --------- | --------- |
| 증가 정확도    | 낮음      | 매우 높음 |
| 캐시–DB 불일치 | 자주 발생 | 없음      |
| 성능(속도)     | 빠름      | 느림      |
| 안정성         | 낮음      | 매우 높음 |
| 구현 난이도    | 매우 쉬움 | 쉬움      |

---

## 9. 결론

- 멀티스레드 환경에서는 단순한 +1 증가 연산조차 안전하지 않음
- 캐시와 DB 구조는 필연적으로 race condition을 만들기 쉬움
- 전체 연산을 lock으로 묶으면 문제는 간단하게 해결됨
- 다만 성능 저하가 있으므로 실제 서비스에서는  
  Redis INCR, DB atomic update, 분산 락 등 더 발전된 구조를 사용해야 함
