package com.irion.guestbook.service;

import com.irion.guestbook.persistence.GuestbookMapper;
import com.irion.guestbook.service.impl.GuestbookServiceImpl;
import com.irion.guestbook.domain.GuestbookVO;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

/** 저장 직전 다듬기와 조회 범위 제한. 매퍼는 가짜라 DB 없이 돈다. */
public class GuestbookServiceImplTest {

    /** DB 로 내려가기 직전의 값을 붙잡아 두는 가짜 매퍼 */
    private static final class Captor {
        GuestbookVO saved;
        int offset = -1;
        int limit = -1;
    }

    @Test
    public void 앞뒤_공백을_떼고_저장한다() {
        Captor captor = new Captor();

        GuestbookVO vo = new GuestbookVO();
        vo.setNickname("  예티125  ");
        vo.setContent("  축하해요!  ");
        vo.setCheer("  반짝이길  ");

        service(captor).createGuestbook(vo);

        assertEquals("예티125", captor.saved.getNickname());
        assertEquals("축하해요!", captor.saved.getContent());
        assertEquals("반짝이길", captor.saved.getCheer());
    }

    /** 공백만 넣은 응원 한마디가 '' 로 저장되면 카드에 빈 줄이 생긴다 */
    @Test
    public void 공백만_있는_응원_한마디는_NULL_이_된다() {
        Captor captor = new Captor();

        GuestbookVO vo = new GuestbookVO();
        vo.setNickname("예티");
        vo.setContent("축하해요");
        vo.setCheer("     ");

        service(captor).createGuestbook(vo);

        assertNull(captor.saved.getCheer());
    }

    /** 주소로 limit 을 키워 통째로 긁어가는 것을 막는다 */
    @Test
    public void 한_번에_가져갈_수_있는_개수에_상한이_있다() {
        Captor captor = new Captor();

        service(captor).getGuestbookList(0, 100000);

        assertEquals(50, captor.limit);
    }

    @Test
    public void limit_이_0_이하면_기본값으로_되돌린다() {
        Captor captor = new Captor();

        service(captor).getGuestbookList(0, 0);

        assertEquals(12, captor.limit);
    }

    /** 음수 offset 은 SQL 문법 오류가 되어 500 이 난다 */
    @Test
    public void 음수_offset_은_0_으로_눕힌다() {
        Captor captor = new Captor();

        service(captor).getGuestbookList(-5, 12);

        assertEquals(0, captor.offset);
    }

    @Test
    public void 아이디가_없으면_삭제를_시도하지_않는다() {
        Captor captor = new Captor();

        assertFalse(service(captor).deleteGuestbook(null));
    }

    private static GuestbookService service(Captor captor) {
        GuestbookMapper mapper = (GuestbookMapper) Proxy.newProxyInstance(
                GuestbookMapper.class.getClassLoader(),
                new Class<?>[] { GuestbookMapper.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insertGuestbook".equals(name)) {
                        GuestbookVO vo = (GuestbookVO) args[0];
                        captor.saved = vo;
                        vo.setGuestbookId(1L);
                        return 1;
                    }
                    if ("selectGuestbookList".equals(name)) {
                        captor.offset = (Integer) args[0];
                        captor.limit = (Integer) args[1];
                        return new java.util.ArrayList<GuestbookVO>();
                    }
                    if ("deleteGuestbook".equals(name)) {
                        return 1;
                    }
                    return method.getReturnType() == int.class ? 0 : null;
                });

        GuestbookServiceImpl service = new GuestbookServiceImpl();
        try {
            Field field = GuestbookServiceImpl.class.getDeclaredField("guestbookMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("guestbookMapper 필드를 찾지 못했다", e);
        }
        return service;
    }
}
