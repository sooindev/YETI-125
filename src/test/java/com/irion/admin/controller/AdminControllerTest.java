package com.irion.admin.controller;

import com.irion.admin.vo.AdminVO;
import com.irion.common.util.JsonResult;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

/**
 * 로그인 화면과 로그인 처리의 입구.
 *
 * HttpSession 을 파라미터로 받으면 스프링이 없는 세션을 만들어 넣는다.
 * 누구나 열 수 있는 경로라 그것만으로 빈 세션이 쌓인다.
 */
public class AdminControllerTest {

    @Test
    public void 로그인_페이지를_열어도_세션을_만들지_않는다() {
        boolean[] created = { false };
        HttpServletRequest request = request(created, null);

        String view = new AdminController().loginPage(request);

        assertEquals("redirect:/admin/admin-login.html", view);
        assertFalse("세션을 새로 만들면 안 된다", created[0]);
    }

    @Test
    public void 세션은_있지만_로그인_전이면_로그인_화면으로_보낸다() {
        boolean[] created = { false };
        HttpServletRequest request = request(created, session(null));

        assertEquals("redirect:/admin/admin-login.html",
                new AdminController().loginPage(request));
    }

    @Test
    public void 이미_로그인돼_있으면_관리_화면으로_보낸다() {
        boolean[] created = { false };
        HttpServletRequest request = request(created, session(new AdminVO()));

        assertEquals("redirect:/admin/admin-schedule.html",
                new AdminController().loginPage(request));
    }

    /**
     * adminService 를 꽂지 않은 컨트롤러로도 통과한다는 것 자체가
     * DB 조회 앞에서 끊긴다는 뜻이다.
     */
    @Test
    public void 있을_수_없는_길이의_아이디는_먼저_끊는다() {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            huge.append('a');
        }

        boolean[] created = { false };
        JsonResult result = new AdminController()
                .loginProc(huge.toString(), "pw", request(created, null));

        assertFalse(result.isSuccess());
        assertEquals("아이디 또는 비밀번호가 일치하지 않습니다.", result.getMessage());
        assertFalse("세션도 만들지 않는다", created[0]);
    }

    // ========================================

    /** getSession(false) 는 existing 을, 그 밖은 "만들었다" 로 기록 */
    private static HttpServletRequest request(boolean[] created, HttpSession existing) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    if (!"getSession".equals(method.getName())) {
                        return blank(method.getReturnType());
                    }
                    boolean create = args == null || args.length == 0 || Boolean.TRUE.equals(args[0]);
                    if (create) {
                        created[0] = true;
                        return existing != null ? existing : session(null);
                    }
                    return existing;
                });
    }

    private static HttpSession session(AdminVO adminUser) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class },
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && "adminUser".equals(args[0])) {
                        return adminUser;
                    }
                    return blank(method.getReturnType());
                });
    }

    /** 프록시가 기본형 반환 메서드를 물어봐도 터지지 않도록 */
    private static Object blank(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}
