# 🧵 Multithread View Count Lab

### 캐시–DB 조회수 불일치 및 Race Condition 실험 프로젝트

이 프로젝트는 멀티스레드 환경에서 **조회수 증가 로직**을 테스트하여  
캐시(Cache)와 데이터베이스(DB) 사이에서 발생하는 **타이밍 이슈**,  
**lost update**, **race condition**, **캐시–DB 불일치 문제**를 직접 재현하고  
`Lock`을 적용하여 문제를 해결하는 과정을 실험하기 위한 코드입니다.

---

## 1. 실험 목적

- 여러 스레드가 동시에 조회 API를 호출할 때 발생하는  
  **race condition** 현상을 이해한다.
- 캐시와 DB 값이 **불일치하는 문제**를 직접 구현하고 확인한다.
- 락(`synchronized`)을 적용해  
  **정합성(consistency) 문제가 어떻게 해결되는지** 실험·비교한다.
- 단순한 인메모리 구조로 멀티스레드 동작을 명확하게 설명하는 것이 목표이며  
  복잡한 인프라(DB·Redis)는 사용하지 않는다.

---

## 📁 2. 프로젝트 구조

```
view-count-project
│── FakeDatabase.java      # 인메모리 DB 역할
│── FakeCache.java         # 인메모리 캐시 역할
│── ViewCountService.java  # 조회 로직 (lock 없음 / lock 있음)
│── ViewCountApp.java      # 멀티스레드 테스트 실행
└── README.md
```

---

## ⚙️ 3. 조회 API 동작 순서

과제에서 제시된 프로세스를 그대로 구현하였습니다.

1. 캐시에서 조회수 읽기
2. 캐시에 값이 없으면 DB에서 조회 후 캐시에 저장
3. 조회수 +1 증가
4. DB 저장
5. 캐시 저장
6. 최종 조회수 반환

---

## 🔥 4. 멀티스레드 테스트 환경

| 항목               | 값                                 |
| ------------------ | ---------------------------------- |
| 스레드 수          | 20                                 |
| 스레드당 반복 횟수 | 500                                |
| 기대 조회수        | **10,000**                         |
| 테스트 방식        | `ExecutorService + CountDownLatch` |

테스트는 두 단계로 진행됩니다:

### 1) `increaseViewCount()`

→ **LOCK 없음 (문제 재현)**

### 2) `increaseViewCountWithLock()`

→ **LOCK 있음 (해결 후 비교)**

---

## ❌ 5. LOCK 없음 결과 (문제 재현)

```
===== Naive Version Result (LOCK YO'Q) =====
Expected total calls: 10000
DB viewCount        : 510
Cache viewCount     : 510
Time(ms)            : 776
```

### ✔ 문제 분석

- 기대값: **10,000**, 실제 DB 값: **510**
- **약 9,490번의 증가 연산이 사라짐 → lost update**
- 캐시–DB 값 불일치가 여러 상황에서 발생
- race condition과 timing issue가 명확히 드러남

---

## ✅ 6. LOCK 적용 결과 (문제 해결)

```
===== Locked Version Result (LOCK BOR) =====
Expected total calls: 50000
DB viewCount        : 50000
Cache viewCount     : 50000
Time(ms)            : 77741
```

### ✔ 개선 분석

- DB = Cache = Expected
- 증가 연산 100% 보장됨
- 캐시–DB 불일치가 완전히 사라짐
- 실행 시간은 증가하나 정합성은 완벽

---

## 🧠 7. 핵심 개념 요약

### 🔸 Lost Update

여러 스레드가 **같은 캐시 값을 읽고**,  
증가 후 서로 덮어쓰면서 실제 증가 횟수가 사라지는 현상.

### 🔸 Race Condition

동시에 공유 데이터에 접근하면서  
실행 순서에 따라 잘못된 값이 저장되는 문제.

### 🔸 캐시–DB 불일치

DB와 캐시의 업데이트 타이밍 차이로  
값이 서로 다르게 유지되는 현상.

### 🔸 해결 전략: 전체 연산 Lock

`synchronized(lock)`  
→ 한 번에 하나의 스레드만 실행  
→ 전체 증가 로직이 원자적으로 처리됨

---

## 🏁 8. 결론

- 멀티스레드 환경에서는 단순한 +1 증가 연산조차 안전하지 않다.
- 캐시–DB 구조에서는 **race condition**이 매우 쉽게 발생한다.
- 전체 연산에 Lock을 적용하면  
  lost update 및 캐시–DB 불일치 문제를 완전히 해결할 수 있다.
- 실제 서비스에서는 Redis INCR, Atomic operation, 분산락 등을 고려해야 한다.

---

## 🔗 9. 실행 방법

```
javac *.java
java ViewCountApp
```

---

## 👨‍💻 Author

Sardorbek Madgoziev (사르도르벡 마드고지예브)  
Multithread & Concurrency Study Project
