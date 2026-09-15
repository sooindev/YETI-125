package com.irion.common.web;

/**
 * 사이트 정규 주소. canonical · og:url · 사이트맵이 절대 주소를 쓴다.
 * Host 헤더에서 만들지 않는 이유 — 조작된 Host 가 canonical 에 실리면
 * 검색엔진에 남의 주소를 정답으로 알려준다
 */
public final class Site {

    public static final String URL = "https://yeti-125.com";

    private Site() {
    }

    /** 정규 주소 + 경로. path 는 "/" 로 시작 */
    public static String url(String path) {
        return URL + path;
    }
}
