package com.irion.feature.guestbook.service.impl;

import com.irion.feature.guestbook.persistence.GuestbookMapper;
import com.irion.feature.guestbook.service.GuestbookService;
import com.irion.feature.guestbook.domain.GuestbookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GuestbookServiceImpl implements GuestbookService {

    /** 응답 최대 개수. limit 을 키워 통째로 긁어가는 것을 방지 */
    private static final int MAX_LIMIT = 50;

    @Autowired
    private GuestbookMapper guestbookMapper;

    /** 목록 — 최신순, 페이지 단위 */
    @Override
    public List<GuestbookVO> getGuestbookList(int offset, int limit) {
        return guestbookMapper.selectGuestbookList(clampOffset(offset), clampLimit(limit));
    }

    /** 미삭제 건수 */
    @Override
    public int getGuestbookCount() {
        return guestbookMapper.selectGuestbookCount();
    }

    /** 등록 */
    @Override
    @Transactional
    public Long createGuestbook(GuestbookVO guestbook) {
        trim(guestbook);

        int result = guestbookMapper.insertGuestbook(guestbook);
        return result > 0 ? guestbook.getGuestbookId() : null;
    }

    /**
     * 소프트 삭제 — 행은 남긴다.
     * 오삭제 복구가 가능해야 하고, 일정 테이블도 같은 방식이라 운영을 통일
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
     * 앞뒤 공백 제거, 빈 문자열이 된 선택 항목은 NULL 로.
     * 공백만 든 응원이 '' 로 저장되면 화면에 빈 줄이 생긴다
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

    /** 0 이하 → 기본값, 상한 초과 → 상한 */
    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 12;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
