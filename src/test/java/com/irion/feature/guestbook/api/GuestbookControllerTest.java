package com.irion.feature.guestbook.api;

import com.irion.common.api.JsonResult;
import com.irion.feature.guestbook.service.GuestbookService;
import com.irion.feature.guestbook.domain.GuestbookVO;
import com.irion.testsupport.FakeHttp;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * 공개 방명록 API 의 약속.
 *
 * 화면에서 입력칸을 없애는 것은 방어가 아니다 — 주소만 알면 아무 값이나 실어 보낼 수 있다.
 * 이름과 식별자를 서버가 다시 정한다는 것을 여기서 못 박는다.
 */
public class GuestbookControllerTest {

    /** DB 로 내려가기 직전의 VO 를 붙잡아 두는 가짜 서비스 */
    private static final class Captor {
        GuestbookVO saved;
    }

    @Test
    public void 실려_온_닉네임은_무시하고_익명으로_저장한다() {
        Captor captor = new Captor();
        GuestbookController controller = controller(captor);

        GuestbookVO sent = new GuestbookVO();
        sent.setNickname("관리자");     // 남의 이름을 사칭해 보낸다
        sent.setContent("축하해요");

        JsonResult result = controller.createGuestbook(sent, binding(sent), request(FakeHttp.session()));

        assertTrue(result.isSuccess());
        assertEquals("익명", captor.saved.getNickname());
    }

    /** 아이디를 실어 보내 남의 글을 덮어쓰려는 시도를 막는다 */
    @Test
    public void 실려_온_아이디와_삭제표시는_버린다() {
        Captor captor = new Captor();
        GuestbookController controller = controller(captor);

        GuestbookVO sent = new GuestbookVO();
        sent.setGuestbookId(99L);
        sent.setDelYn("Y");
        sent.setContent("축하해요");

        controller.createGuestbook(sent, binding(sent), request(FakeHttp.session()));

        assertNull(captor.saved.getGuestbookId());
        assertNull(captor.saved.getDelYn());
    }

    /** 새로고침 도배를 막는 간격 제한. 두 번째 글은 거절돼야 한다 */
    @Test
    public void 연달아_올리면_두_번째는_거절한다() {
        Captor captor = new Captor();
        GuestbookController controller = controller(captor);
        HttpSession session = FakeHttp.session();

        GuestbookVO first = new GuestbookVO();
        first.setContent("축하해요");
        assertTrue(controller.createGuestbook(first, binding(first), request(session)).isSuccess());

        GuestbookVO second = new GuestbookVO();
        second.setContent("한 번 더");
        JsonResult result = controller.createGuestbook(second, binding(second), request(session));

        assertFalse("간격 제한에 걸려야 한다", result.isSuccess());
        assertTrue("남은 시간을 알려줘야 한다: " + result.getMessage(),
                result.getMessage().contains("초 후에"));
    }

    /** 목록 응답에 삭제 표시가 섞이면 지운 글의 존재가 드러난다 */
    @Test
    public void 목록은_삭제표시를_내보내지_않는다() {
        Captor captor = new Captor();
        GuestbookController controller = controller(captor);

        JsonResult result = controller.getGuestbookList(0, 12);

        assertTrue(result.isSuccess());
        assertFalse("delYn 이 응답에 있으면 안 된다: " + result.getData(),
                String.valueOf(result.getData()).contains("delYn"));
    }

    // ── 조립 ────────────────────────────────────────────────────

    private static BindingResult binding(GuestbookVO vo) {
        return new BeanPropertyBindingResult(vo, "guestbookVO");
    }

    private static HttpServletRequest request(HttpSession session) {
        return new FakeHttp.Request().uri("/guestbook").method("POST").session(session).build();
    }

    private static GuestbookController controller(Captor captor) {
        GuestbookService service = (GuestbookService) Proxy.newProxyInstance(
                GuestbookService.class.getClassLoader(),
                new Class<?>[] { GuestbookService.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("createGuestbook".equals(name)) {
                        captor.saved = (GuestbookVO) args[0];
                        return 1L;
                    }
                    if ("getGuestbookList".equals(name)) {
                        return new ArrayList<GuestbookVO>();
                    }
                    if ("getGuestbookCount".equals(name)) {
                        return 0;
                    }
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });

        GuestbookController controller = new GuestbookController();
        try {
            Field field = GuestbookController.class.getDeclaredField("guestbookService");
            field.setAccessible(true);
            field.set(controller, service);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("guestbookService 필드를 찾지 못했다", e);
        }
        return controller;
    }
}
