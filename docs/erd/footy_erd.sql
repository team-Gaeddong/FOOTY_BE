-- FOOTY ERD 스냅샷
-- 원본: ERD Cloud "footy" 다이어그램 (2026-10-14 Import한 DDL, 2026-10-19 member 변경 반영)
-- 원본과 다르면 원본이 우선이다. 차이는 docs/harness/decisions.md C-12, C-15 참고.
-- 컬럼·NULL·DEFAULT·FK는 원본 Export와 일치.
-- UNIQUE·AUTO_INCREMENT의 기준은 docs/erd/constraints.md (원본 ERD Cloud에 표기 불가).
-- 원본 URL 및 검증 범위: docs/harness/sources.md (원본 Export 전체 대조 미완료).

CREATE TABLE member (
  member_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
  email VARCHAR(100) NOT NULL COMMENT '이메일',
  nickname VARCHAR(30) NULL COMMENT '닉네임(온보딩 전 NULL)',
  rating DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '누적 실력 점수',
  is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '탈퇴 여부',
  created_at DATETIME NOT NULL COMMENT '가입일',
  deleted_at DATETIME NULL COMMENT '탈퇴일',
  provider VARCHAR(20) NOT NULL COMMENT '로그인 제공자(GOOGLE)',
  provider_id VARCHAR(255) NOT NULL COMMENT '제공자 회원 식별자(sub)',
  is_onboarded TINYINT(1) NOT NULL DEFAULT 0 COMMENT '온보딩 완료 여부',
  PRIMARY KEY (member_id),
  UNIQUE KEY uk_member_email (email),
  UNIQUE KEY uk_member_provider (provider, provider_id)
) COMMENT '회원';

CREATE TABLE member_position (
  member_position_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선호 포지션 ID',
  member_id BIGINT NOT NULL COMMENT '회원 ID',
  position VARCHAR(10) NOT NULL COMMENT '포지션(GK/DF/MF/FW)',
  priority INT NOT NULL COMMENT '선호 순위',
  PRIMARY KEY (member_position_id),
  CONSTRAINT fk_member_position_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) COMMENT '회원 선호 포지션';

CREATE TABLE game (
  game_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '경기 ID',
  host_id BIGINT NOT NULL COMMENT '파티장(생성자) ID',
  title VARCHAR(100) NOT NULL COMMENT '경기 제목',
  play_date DATETIME NOT NULL COMMENT '경기 일시',
  location VARCHAR(200) NOT NULL COMMENT '장소',
  max_players INT NOT NULL COMMENT '최대 인원',
  team_count INT NOT NULL DEFAULT 2 COMMENT '팀 수',
  status VARCHAR(20) NOT NULL COMMENT '상태(RECRUITING/CONFIRMED/FINISHED)',
  is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부',
  created_at DATETIME NOT NULL COMMENT '생성일',
  PRIMARY KEY (game_id),
  CONSTRAINT fk_game_host FOREIGN KEY (host_id) REFERENCES member (member_id)
) COMMENT '경기';

CREATE TABLE game_player (
  game_player_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '경기 참가자 ID',
  game_id BIGINT NOT NULL COMMENT '경기 ID',
  member_id BIGINT NULL COMMENT '회원 ID(비회원 선수는 NULL)',
  name VARCHAR(30) NOT NULL COMMENT '선수 이름',
  skill_level INT NULL COMMENT '실력 점수(파티장 평가)',
  is_substitute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '후보 선수 여부',
  joined_at DATETIME NOT NULL COMMENT '참여일',
  PRIMARY KEY (game_player_id),
  UNIQUE KEY uk_game_player_member (game_id, member_id),
  CONSTRAINT fk_game_player_game FOREIGN KEY (game_id) REFERENCES game (game_id),
  CONSTRAINT fk_game_player_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) COMMENT '경기 참가 선수';

CREATE TABLE player_position (
  player_position_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선수 포지션 ID',
  game_player_id BIGINT NOT NULL COMMENT '경기 참가자 ID',
  position VARCHAR(10) NOT NULL COMMENT '포지션',
  priority INT NOT NULL COMMENT '희망 순위',
  PRIMARY KEY (player_position_id),
  CONSTRAINT fk_player_position_player FOREIGN KEY (game_player_id) REFERENCES game_player (game_player_id)
) COMMENT '경기별 선수 희망 포지션(복수 선택)';

CREATE TABLE lineup (
  lineup_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '라인업 ID',
  game_id BIGINT NOT NULL COMMENT '경기 ID',
  version INT NOT NULL COMMENT '팀 짜기 회차(다시 짜기 시 증가)',
  method VARCHAR(20) NOT NULL COMMENT '생성 방식(AI/ALGORITHM/MANUAL)',
  is_confirmed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '팀 확정 여부',
  image_url VARCHAR(500) NULL COMMENT '전술 배치도 이미지',
  created_at DATETIME NOT NULL COMMENT '생성일',
  PRIMARY KEY (lineup_id),
  CONSTRAINT fk_lineup_game FOREIGN KEY (game_id) REFERENCES game (game_id)
) COMMENT '팀 구성안';

CREATE TABLE team (
  team_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '팀 ID',
  lineup_id BIGINT NOT NULL COMMENT '라인업 ID',
  name VARCHAR(30) NOT NULL COMMENT '팀 이름',
  formation VARCHAR(20) NULL COMMENT '포메이션',
  total_skill INT NULL COMMENT '팀 실력 합계',
  PRIMARY KEY (team_id),
  CONSTRAINT fk_team_lineup FOREIGN KEY (lineup_id) REFERENCES lineup (lineup_id)
) COMMENT '팀';

CREATE TABLE team_member (
  team_member_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '팀 배정 ID',
  team_id BIGINT NOT NULL COMMENT '팀 ID',
  game_player_id BIGINT NOT NULL COMMENT '경기 참가자 ID',
  position VARCHAR(10) NOT NULL COMMENT '배정 포지션',
  pos_x DECIMAL(5,2) NULL COMMENT '배치도 X 좌표',
  pos_y DECIMAL(5,2) NULL COMMENT '배치도 Y 좌표',
  PRIMARY KEY (team_member_id),
  CONSTRAINT fk_team_member_team FOREIGN KEY (team_id) REFERENCES team (team_id),
  CONSTRAINT fk_team_member_player FOREIGN KEY (game_player_id) REFERENCES game_player (game_player_id)
) COMMENT '팀 배정 및 전술 배치';

CREATE TABLE balance_review (
  balance_review_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '밸런스 평가 ID',
  game_id BIGINT NOT NULL COMMENT '경기 ID',
  writer_id BIGINT NOT NULL COMMENT '작성자 ID',
  score INT NOT NULL COMMENT '밸런스 점수(1~5)',
  comment VARCHAR(500) NULL COMMENT '의견',
  created_at DATETIME NOT NULL COMMENT '작성일',
  PRIMARY KEY (balance_review_id),
  UNIQUE KEY uk_balance_review (game_id, writer_id),
  CONSTRAINT fk_balance_review_game FOREIGN KEY (game_id) REFERENCES game (game_id),
  CONSTRAINT fk_balance_review_writer FOREIGN KEY (writer_id) REFERENCES member (member_id)
) COMMENT '경기 이후 밸런스 평가';

CREATE TABLE mvp_vote (
  mvp_vote_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'MVP 투표 ID',
  game_id BIGINT NOT NULL COMMENT '경기 ID',
  voter_id BIGINT NOT NULL COMMENT '투표자 ID',
  game_player_id BIGINT NOT NULL COMMENT 'MVP 후보(참가자) ID',
  created_at DATETIME NOT NULL COMMENT '투표일',
  PRIMARY KEY (mvp_vote_id),
  UNIQUE KEY uk_mvp_vote (game_id, voter_id),
  CONSTRAINT fk_mvp_vote_game FOREIGN KEY (game_id) REFERENCES game (game_id),
  CONSTRAINT fk_mvp_vote_voter FOREIGN KEY (voter_id) REFERENCES member (member_id),
  CONSTRAINT fk_mvp_vote_player FOREIGN KEY (game_player_id) REFERENCES game_player (game_player_id)
) COMMENT '경기 MVP 투표';

CREATE TABLE notice (
  notice_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공지 ID',
  game_id BIGINT NOT NULL COMMENT '경기 ID',
  writer_id BIGINT NOT NULL COMMENT '작성자(파티장) ID',
  title VARCHAR(100) NOT NULL COMMENT '제목',
  content TEXT NOT NULL COMMENT '내용',
  created_at DATETIME NOT NULL COMMENT '작성일',
  PRIMARY KEY (notice_id),
  CONSTRAINT fk_notice_game FOREIGN KEY (game_id) REFERENCES game (game_id),
  CONSTRAINT fk_notice_writer FOREIGN KEY (writer_id) REFERENCES member (member_id)
) COMMENT '파티장 공지';

CREATE TABLE poll (
  poll_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '투표 ID',
  game_id BIGINT NOT NULL COMMENT '경기 ID',
  creator_id BIGINT NOT NULL COMMENT '생성자 ID',
  title VARCHAR(100) NOT NULL COMMENT '투표 제목',
  allow_multiple TINYINT(1) NOT NULL DEFAULT 0 COMMENT '복수 선택 허용',
  deadline DATETIME NULL COMMENT '마감일',
  created_at DATETIME NOT NULL COMMENT '생성일',
  PRIMARY KEY (poll_id),
  CONSTRAINT fk_poll_game FOREIGN KEY (game_id) REFERENCES game (game_id),
  CONSTRAINT fk_poll_creator FOREIGN KEY (creator_id) REFERENCES member (member_id)
) COMMENT '의견 투표';

CREATE TABLE poll_option (
  poll_option_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택지 ID',
  poll_id BIGINT NOT NULL COMMENT '투표 ID',
  content VARCHAR(100) NOT NULL COMMENT '선택지 내용',
  PRIMARY KEY (poll_option_id),
  CONSTRAINT fk_poll_option_poll FOREIGN KEY (poll_id) REFERENCES poll (poll_id)
) COMMENT '투표 선택지';

CREATE TABLE poll_answer (
  poll_answer_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '응답 ID',
  poll_option_id BIGINT NOT NULL COMMENT '선택지 ID',
  member_id BIGINT NOT NULL COMMENT '응답자 ID',
  created_at DATETIME NOT NULL COMMENT '응답일',
  PRIMARY KEY (poll_answer_id),
  UNIQUE KEY uk_poll_answer (poll_option_id, member_id),
  CONSTRAINT fk_poll_answer_option FOREIGN KEY (poll_option_id) REFERENCES poll_option (poll_option_id),
  CONSTRAINT fk_poll_answer_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) COMMENT '투표 응답';

CREATE TABLE report (
  report_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '신고 ID',
  reporter_id BIGINT NOT NULL COMMENT '신고자 ID',
  reported_id BIGINT NOT NULL COMMENT '피신고자 ID',
  game_id BIGINT NULL COMMENT '관련 경기 ID',
  reason VARCHAR(50) NOT NULL COMMENT '신고 사유',
  detail VARCHAR(500) NULL COMMENT '상세 내용',
  status VARCHAR(20) NOT NULL COMMENT '처리 상태(PENDING/DONE)',
  created_at DATETIME NOT NULL COMMENT '신고일',
  PRIMARY KEY (report_id),
  CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES member (member_id),
  CONSTRAINT fk_report_reported FOREIGN KEY (reported_id) REFERENCES member (member_id),
  CONSTRAINT fk_report_game FOREIGN KEY (game_id) REFERENCES game (game_id)
) COMMENT '악성 유저 신고';
