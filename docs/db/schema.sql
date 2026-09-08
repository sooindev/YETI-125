-- 데이터베이스 생성
CREATE DATABASE for_125
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 데이터베이스 선택
USE for_125;

-- 관리자 테이블
CREATE TABLE tb_admin (
    admin_id         BIGINT       NOT NULL AUTO_INCREMENT,
    admin_login_id   VARCHAR(50)  NOT NULL,
    admin_password   VARCHAR(200) NOT NULL,
    admin_name       VARCHAR(50)  NOT NULL,
    last_login_date  DATETIME     NULL,
    reg_date         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn           CHAR(1)      NOT NULL DEFAULT 'N',
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_admin_login_id (admin_login_id)
);

-- 일정 테이블
CREATE TABLE tb_schedule (
    schedule_id    BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL,
    description    TEXT         NULL,
    schedule_type  VARCHAR(20)  NOT NULL DEFAULT 'STREAM',
    start_date     DATETIME     NOT NULL,
    end_date       DATETIME     NULL,
    all_day_yn     CHAR(1)      NOT NULL DEFAULT 'N',
    display_yn     CHAR(1)      NOT NULL DEFAULT 'Y',
    color          VARCHAR(10)  NULL DEFAULT '#6366F1',
    reg_date       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn         CHAR(1)      NOT NULL DEFAULT 'N',
    PRIMARY KEY (schedule_id),
    INDEX idx_schedule_date (start_date, end_date)
);

-- 방명록 테이블 (데뷔 3주년 기념)
--
-- 이미 돌고 있는 DB 에는 이 CREATE 문만 따로 실행하면 된다.
-- 다른 테이블과 달리 mod_date 가 없다 — 방명록은 쓰고 나면 고치지 않는다.
CREATE TABLE tb_guestbook (
    guestbook_id  BIGINT       NOT NULL AUTO_INCREMENT,
    nickname      VARCHAR(30)  NOT NULL,
    content       VARCHAR(500) NOT NULL,
    cheer         VARCHAR(100) NULL,
    reg_date      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_yn        CHAR(1)      NOT NULL DEFAULT 'N',
    PRIMARY KEY (guestbook_id),
    -- 목록은 "안 지워진 것을 최신순으로" 만 읽는다. 두 컬럼을 이 순서로 묶어야
    -- 걸러내기와 정렬이 한 인덱스에서 끝난다
    INDEX idx_guestbook_list (del_yn, guestbook_id)
);

-- 테이블 확인
SHOW TABLES;