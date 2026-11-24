# sb-grader — 주차별 채점 테스트

SpringBoot 과목 실습 저장소(`sb-<학번>`)의 GitHub Actions 가 **매번 이 저장소를 내려받아**
`weekNN/` 안의 테스트를 학생 코드에 얹어 실행한다.

학생 저장소는 건드리지 않는다. 여기에 주차 폴더만 추가하면 다음 push 부터 전원에게 적용된다.

---

## 매주 하는 일

1. `weekNN/` 폴더를 만든다
2. 테스트 클래스를 넣는다 — **패키지는 반드시 `grader.weekNN`**
3. 커밋 · push

끝이다. 학생에게 공지할 것도 없다.

---

## 테스트 작성 규칙

**① 패키지는 `com.example.blog.grader.weekNN`**

`@SpringBootTest` 는 테스트 패키지에서 **위로 올라가며** `@SpringBootApplication` 을 찾는다.
학생 앱이 `com.example.blog` 이므로 그 하위에 있어야 컨텍스트를 찾는다.
`grader.weekNN` 처럼 최상위에 두면 `Unable to find a @SpringBootConfiguration` 으로 전부 실패한다.

채점 요약은 패키지명에서 `weekNN` 을 읽어 주차를 구분한다.

**② HTTP 로만 검증한다**
학생의 패키지명 · 클래스명 · 내부 구현에 의존하지 않는다.
`@Autowired MockMvc` 만 쓰고, 리포지토리나 엔티티를 import 하지 않는다.

```java
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // CH08 시큐리티 적용 후에도 로그인 없이 채점
class WeekNNTest {
    @Autowired MockMvc mockMvc;
}
```

**③ 자기 데이터는 자기가 만든다**
`deleteAll()` 로 DB 를 비우지 말 것. 다른 주차 테스트와 충돌한다.
고유한 제목(`"t" + System.nanoTime()`)을 써서 남의 데이터와 섞이지 않게 한다.

**④ `@DisplayName` 이 학생이 보는 문장이다**
채점표에 그대로 찍힌다. 무엇을 고쳐야 할지 알 수 있게 쓸 것.

```
[4주차] DELETE /api/articles/{id} 로 삭제하면 목록에서 사라진다
```

---

## 배점

테스트 하나당 같은 배점, 합계 100 점. 바꾸려면 `scripts/summarize.py` 를 고친다.

## 현재 등록된 주차

| 폴더 | 범위 | 테스트 |
|---|---|---|
| `week03/` | CH05 글 등록 | 1 |
| `week04/` | CH05 조회 · 수정 · 삭제 | 4 |
| `week12/` | CH10 구글 로그인 — client-id · secret 커밋 + 구글 콘솔 등록 확인 (실제 로그인은 스크린샷) | 3 |

## CH10 구글 로그인 채점 (week12)

- 학생은 client-id · client-secret 을 모두 `application.yml` 에 커밋한다(학생 저장소는 private, 2026-11-24 결정).
- ① 앱이 구글로 보내는 client_id 가 구글 형식인지 ② 구글이 그 client_id + `http://localhost:8080/login/oauth2/code/google` 을 받아 주는지(로그인 화면으로 이동 vs `/signin/oauth/error`) 확인.
- ③ 가짜 인가 코드로 토큰 요청 → secret 이 맞으면 `invalid_grant`, 틀리면 `invalid_client`.
- 실제 로그인 성공(구글 계정 인증)은 CI 에서 불가 → 로그인 후 화면 스크린샷으로 별도 확인.
- 이 테스트만 시큐리티 필터를 켠다(`@AutoConfigureMockMvc` 기본값). 러너에서 accounts.google.com 으로 나가는 네트워크가 필요하다.
