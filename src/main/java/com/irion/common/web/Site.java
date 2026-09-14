package com.irion.common.web;

/**
 * 사이트의 정규 주소. 상세 화면의 canonical · og:url 과 사이트맵이 절대 주소를 쓴다.
 *
 * 요청의 Host 헤더에서 만들지 않는 이유는, 그 값이 바깥에서 오기 때문이다 —
 * 조작된 Host 가 그대로 canonical 에 실리면 검색엔진에 남의 주소를 정답이라고 알려 준다.
 */
public final class Site {

    public static final String URL = "https://yeti-125.com";

    private Site() {
    }

    /** 경로를 정규 주소에 잇는다. path 는 "/" 로 시작해야 한다 */
    public static String url(String path) {
        return URL + path;
    }
}
