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

-- 테이블 확인
SHOW TABLES;