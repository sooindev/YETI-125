package com.irion.guestbook.persistence;

import com.irion.guestbook.domain.GuestbookVO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** 매퍼 XML 이 만드는 SQL 문장. 서비스 테스트는 매퍼가 가짜라 못 잡아 DB 없이 문장만 본다. */
public class GuestbookSqlTest {

    private static final String NS = "com.irion.guestbook.persistence.GuestbookMapper.";

    private static Configuration config;

    @BeforeClass
    public static void 매퍼_XML_을_읽는다() throws Exception {
        config = new Configuration();
        // 형식상 필요할 뿐 연결은 열지 않는다
        config.setEnvironment(new Environment(
                "test", new JdbcTransactionFactory(), new UnpooledDataSource()));
        config.getTypeAliasRegistry().registerAlias("GuestbookVO", GuestbookVO.class);

        String resource = "sql/guestbook/Guestbook_SQL.xml";
        try (InputStream in = GuestbookSqlTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull("매퍼 XML 을 찾지 못했다: " + resource, in);
            new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
        }
    }

    /** 지운 글이 목록에 섞여 나오면 관리자가 지운 의미가 없다 */
    @Test
    public void 목록은_지워진_글을_거른다() {
        String sql = sql("selectGuestbookList", params(0, 12));

        assertTrue("del_yn 조건이 있어야 한다: " + sql, sql.contains("del_yn = 'N'"));
    }

    /**
     * 정렬을 reg_date 로 하면 같은 초에 들어온 글끼리 순서가 흔들려
     * 페이지 경계에서 글이 중복되거나 빠진다. id 로 고정해야 한다.
     */
    @Test
    public void 목록은_아이디_역순으로_정렬한다() {
        String sql = sql("selectGuestbookList", params(0, 12));

        assertTrue("guestbook_id DESC 로 정렬해야 한다: " + sql,
                sql.replaceAll("\\s+", " ").contains("ORDER BY guestbook_id DESC"));
    }

    @Test
    public void 목록은_LIMIT_과_OFFSET_을_바인딩한다() {
        String sql = sql("selectGuestbookList", params(24, 12)).replaceAll("\\s+", " ");

        // 값을 문자열로 이어 붙이면 SQL 인젝션이 된다 — 반드시 ? 바인딩이어야 한다
        assertTrue("LIMIT/OFFSET 이 바인딩이어야 한다: " + sql, sql.contains("LIMIT ? OFFSET ?"));
    }

    @Test
    public void 개수도_지워진_글을_거른다() {
        String sql = sql("selectGuestbookCount", new HashMap<String, Object>());

        assertTrue("del_yn 조건이 있어야 한다: " + sql, sql.contains("del_yn = 'N'"));
    }

    /** 삭제는 소프트 삭제다 — DELETE 문이면 되살릴 수 없다 */
    @Test
    public void 삭제는_행을_지우지_않고_표시만_바꾼다() {
        Map<String, Object> args = new HashMap<>();
        args.put("guestbookId", 1L);

        String sql = sql("deleteGuestbook", args).replaceAll("\\s+", " ");

        assertTrue("UPDATE 여야 한다: " + sql, sql.startsWith("UPDATE tb_guestbook"));
        assertTrue("del_yn 을 Y 로 바꿔야 한다: " + sql, sql.contains("SET del_yn = 'Y'"));
        assertFalse("DELETE 문이면 안 된다: " + sql, sql.contains("DELETE FROM"));
    }

    private static Map<String, Object> params(int offset, int limit) {
        Map<String, Object> args = new HashMap<>();
        args.put("offset", offset);
        args.put("limit", limit);
        return args;
    }

    private static String sql(String id, Object parameter) {
        MappedStatement statement = config.getMappedStatement(NS + id);
        BoundSql boundSql = statement.getBoundSql(parameter);
        return boundSql.getSql().trim();
    }
}
