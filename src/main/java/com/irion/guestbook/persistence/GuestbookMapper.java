package com.irion.guestbook.persistence;

import com.irion.guestbook.domain.GuestbookVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GuestbookMapper {

    List<GuestbookVO> selectGuestbookList(@Param("offset") int offset, @Param("limit") int limit);

    int selectGuestbookCount();

    int insertGuestbook(GuestbookVO guestbookVO);

    int deleteGuestbook(@Param("guestbookId") Long guestbookId);

}
