package com.irion.guestbook.service.impl;

import com.irion.guestbook.mapper.GuestbookMapper;
import com.irion.guestbook.service.GuestbookService;
import com.irion.guestbook.vo.GuestbookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GuestbookServiceImpl implements GuestbookService {

    /** 한 번에 내보내는 최대 개수. 주소로 limit 을 키워 통째로 긁어가는 것을 막는다 */
    private static final int MAX_LIMIT = 50;

    @Autowired
    private GuestbookMapper guestbookMapper;

    /** 방명록 목록 (최신순, 페이지 단위) */
    @Override
    public List<GuestbookVO> getGuestbookList(int offset, int limit) {
        return guestbookMapper.selectGuestbookList(clampOffset(offset), clampLimit(limit));
    }

    /** 지워지지 않은 방명록 수 */
    @Override
    public int getGuestbookCount() {
        return guestbookMapper.selectGuestbookCount();
    }

    /** 방명록 등록 */
    @Override
    @Transactional
    public Long createGuestbook(GuestbookVO guestbook) {
        trim(guestbook);

        int result = guestbookMapper.insertGuestbook(guestbook);
        return result > 0 ? guestbook.getGuestbookId() : null;
    }

    /**
     * 방명록 삭제 (소프트 삭제).
     * 지운 글도 행은 남는다 — 잘못 지웠을 때 되살릴 수 있어야 하고,
     * 일정 테이블도 같은 방식이라 운영 방법을 하나로 맞춘다.
     */
    @Override
    @Transactional
    public boolean deleteGuestbook(Long guestbookId) {
        if (guestbookId == null) {
            return false;
        }
        return guestbookMapper.deleteGuestbook(guestbookId) > 0;
    }

    /**
     * 앞뒤 공백을 떼고, 빈 문자열이 된 선택 항목은 NULL 로 눕힌다.
     * 공백만 넣은 응원 한마디가 '' 로 저장되면 화면에서 빈 줄이 생긴다.
     */
    private void trim(GuestbookVO guestbook) {
        if (guestbook.getNickname() != null) {
            guestbook.setNickname(guestbook.getNickname().trim());
        }
        if (guestbook.getContent() != null) {
            guestbook.setContent(guestbook.getContent().trim());
        }
        if (guestbook.getCheer() != null) {
            String cheer = guestbook.getCheer().trim();
            guestbook.setCheer(cheer.isEmpty() ? null : cheer);
        }
    }

    private int clampOffset(int offset) {
        return Math.max(offset, 0);
    }

    /** 0 이하는 기본값으로, 상한을 넘으면 상한으로 */
    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 12;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
