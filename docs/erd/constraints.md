# ERD 보충 제약 조건

ERD Cloud에는 UNIQUE 제약과 AUTO_INCREMENT를 입력할 수 없어서, 그 기준은 이 문서로 둔다(`docs/harness/decisions.md` C-12, 2026-10-19·21 확정).

- 컬럼·타입·NULL·DEFAULT·FK의 기준은 계속 ERD Cloud `footy`다.
- 엔티티와 DDL은 아래 AUTO_INCREMENT와 UNIQUE 제약을 모두 지켜야 한다. `docs/erd/footy_erd.sql`도 같은 제약을 담는다.
- 제약을 추가·삭제할 때는 이 문서와 `footy_erd.sql`을 함께 바꾸고 PR에서 사람의 승인을 받는다.

## AUTO_INCREMENT

15개 테이블의 PK(`BIGINT`)는 모두 `AUTO_INCREMENT`다. JPA에서는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`로 매핑한다.

## UNIQUE

| 이름 | 테이블 | 컬럼 | 이유 |
|---|---|---|---|
| `uk_member_email` | `member` | `email` | 이메일 1개당 회원 1명 |
| `uk_member_provider` | `member` | `provider`, `provider_id` | Google 계정 1개당 회원 1명 |
| `uk_game_player_member` | `game_player` | `game_id`, `member_id` | 한 경기에 같은 회원이 중복 참가하지 않음(비회원은 `member_id` NULL이라 제약 대상 아님) |
| `uk_balance_review` | `balance_review` | `game_id`, `writer_id` | 한 경기에서 회원당 밸런스 평가 1건 |
| `uk_mvp_vote` | `mvp_vote` | `game_id`, `voter_id` | 한 경기에서 회원당 MVP 투표 1건 |
| `uk_poll_answer` | `poll_answer` | `poll_option_id`, `member_id` | 같은 선택지에 회원당 응답 1건 |

## 미결정

- `uk_member_email`: 탈퇴(소프트 삭제) 후 같은 이메일로 재가입하면 이 제약에 걸림. 재가입 정책이 정해지면 다시 검토.
