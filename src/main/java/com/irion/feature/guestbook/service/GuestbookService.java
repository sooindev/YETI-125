package com.irion.feature.guestbook.service;

import com.irion.feature.guestbook.domain.GuestbookVO;

import java.util.List;

public interface GuestbookService {

    List<GuestbookVO> getGuestbookList(int offset, int limit);

    int getGuestbookCount();

    Long createGuestbook(GuestbookVO guestbookVO);

    boolean deleteGuestbook(Long guestbookId);

}
