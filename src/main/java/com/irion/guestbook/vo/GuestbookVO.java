package com.irion.guestbook.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

public class GuestbookVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 제약은 tb_guestbook 컬럼 정의에서 따왔다. 여기서 안 걸러내면 DB 제약에 걸려 500 이 난다.

    private Long guestbookId;

    /**
     * 글쓴이 이름. 방명록은 전부 익명이라 사용자가 채우지 않는다 —
     * 컨트롤러가 GuestbookController.ANONYMOUS 로 덮어쓴다.
     * 길이 제약만 남겨 둔다(코드가 실수로 긴 값을 넣으면 DB 제약에 걸려 500 이 난다).
     */
    @Size(max = 30, message = "닉네임은 30자를 넘을 수 없습니다.")
    private String nickname;

    @NotBlank(message = "축하 메시지를 입력해 주세요.")
    @Size(max = 500, message = "메시지는 500자를 넘을 수 없습니다.")
    private String content;

    /** 응원의 한마디 — 카드에 한 줄로 얹히는 짧은 문구라 선택 입력이다 */
    @Size(max = 100, message = "응원의 한마디는 100자를 넘을 수 없습니다.")
    private String cheer;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private Date regDate;

    private String delYn;

    public GuestbookVO() {
    }

    public Long getGuestbookId() {
        return guestbookId;
    }

    public void setGuestbookId(Long guestbookId) {
        this.guestbookId = guestbookId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCheer() {
        return cheer;
    }

    public void setCheer(String cheer) {
        this.cheer = cheer;
    }

    public Date getRegDate() {
        return regDate;
    }

    public void setRegDate(Date regDate) {
        this.regDate = regDate;
    }

    public String getDelYn() {
        return delYn;
    }

    public void setDelYn(String delYn) {
        this.delYn = delYn;
    }

    @Override
    public String toString() {
        return "GuestbookVO{" +
                "guestbookId=" + guestbookId +
                ", nickname='" + nickname + '\'' +
                ", regDate=" + regDate +
                '}';
    }
}
