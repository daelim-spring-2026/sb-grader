package com.example.blog.grader.week05;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * CH06 블로그 화면.
 *
 * 화면 테스트는 응답 본문에 값이 들어 있는지만 본다. 태그 구조나 CSS 는 보지 않으므로
 * 디자인을 바꿔도 깨지지 않는다.
 *
 * 글은 API 가 아니라 DB 에 직접 넣는다. CH05 API 가 덜 됐어도 화면 항목은 따로 채점된다.
 * 단, 작성시각 항목만은 JPA 를 타야 값이 채워지므로 등록 API 를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Week05Test {

    @Autowired MockMvc mockMvc;
    @Autowired DataSource dataSource;

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    // ---- 도우미 ----

    /** ARTICLE 테이블에 글을 직접 넣고 id 를 돌려준다. */
    private long insert(String title, String content) throws Exception {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO ARTICLE (title, content) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new IllegalStateException("글을 넣지 못했습니다");
    }

    private String get(String url, Object... vars) throws Exception {
        MvcResult r;
        try {
            r = mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url, vars)).andReturn();
        } catch (Throwable t) {
            // 화면을 그리다 터지면 여기로 온다. 대부분 html 파일이 없거나 타임리프 문법이 틀린 경우다.
            Throwable root = t;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String hint = root.getClass().getSimpleName().contains("TemplateInput")
                    ? "\n  templates 아래에 해당 html 파일이 있는지 확인하세요."
                    + " 컨트롤러가 돌려주는 이름과 파일 이름이 같아야 합니다."
                    : "\n  타임리프 문법이나 모델에 담은 이름을 확인하세요.";
            fail(url + " 화면을 그리다 실패했습니다." + hint
                    + "\n  원인 : " + root.getClass().getSimpleName() + " " + root.getMessage());
            throw new IllegalStateException("unreachable");
        }
        int status = r.getResponse().getStatus();
        if (status != 200) {
            Throwable e = r.getResolvedException();
            fail(url + " 요청이 " + status + " 로 돌아왔습니다."
                    + (e != null ? "\n  원인 : " + e.getClass().getSimpleName() + " " + e.getMessage() : "")
                    + "\n  BlogViewController 와 templates 아래 html 파일을 확인하세요.");
        }
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String unique(String prefix) {
        return prefix + System.nanoTime();
    }

    // ---- 채점 항목 ----

    @Test
    @DisplayName("[5주차] GET /articles 목록 화면이 열리고 글 제목이 보인다")
    void 목록화면() throws Exception {
        String title = unique("목록제목");
        insert(title, "목록내용");

        String body = get("/articles");
        assertTrue(body.contains(title),
                () -> "목록 화면에 방금 넣은 글 제목이 없습니다.\n"
                        + "  BlogViewController 에서 model 에 articles 를 담았는지,\n"
                        + "  articleList.html 에서 th:each 로 출력하는지 확인하세요.");
    }

    @Test
    @DisplayName("[5주차] GET /articles/{id} 상세 화면에 제목과 내용이 보인다")
    void 상세화면() throws Exception {
        String title = unique("상세제목");
        String content = unique("상세내용");
        long id = insert(title, content);

        String body = get("/articles/{id}", id);
        assertTrue(body.contains(title), () -> "상세 화면에 제목이 없습니다.");
        assertTrue(body.contains(content), () -> "상세 화면에 내용이 없습니다.");
    }

    @Test
    @DisplayName("[5주차] GET /new-article 글 등록 화면이 열린다")
    void 등록화면() throws Exception {
        String body = get("/new-article");
        assertTrue(body.toLowerCase(Locale.ROOT).contains("<form")
                        || body.toLowerCase(Locale.ROOT).contains("<input")
                        || body.toLowerCase(Locale.ROOT).contains("<textarea"),
                () -> "등록 화면에 입력 요소가 없습니다. newArticle.html 을 확인하세요.");
    }

    @Test
    @DisplayName("[5주차] GET /new-article?id= 로 열면 기존 글이 채워져 있다")
    void 수정화면() throws Exception {
        String title = unique("수정대상");
        long id = insert(title, "수정대상내용");

        String body = get("/new-article?id=" + id);
        assertTrue(body.contains(title),
                () -> "수정 화면에 기존 제목이 채워지지 않았습니다.\n"
                        + "  newArticle 컨트롤러에서 id 가 있을 때 글을 조회해 model 에 담는지 확인하세요.");
    }

    @Test
    @DisplayName("[5주차] 새로 등록한 글에 작성시각이 채워진다 (@EntityListeners)")
    void 작성시각() throws Exception {
        String title = unique("시각확인");
        MvcResult r = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"content\":\"시각확인내용\"}")).andReturn();
        if (r.getResponse().getStatus() / 100 != 2) {
            fail("글 등록 API 가 " + r.getResponse().getStatus() + " 로 돌아왔습니다."
                    + " 3주차 등록 API 부터 확인하세요.");
        }

        Matcher m = ID.matcher(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        Long id = m.find() ? Long.valueOf(m.group(1)) : null;

        String col = 시각컬럼();
        if (col == null) {
            fail("ARTICLE 테이블에 작성시각 컬럼이 없습니다."
                    + "\n  Article 에 @CreatedDate 가 붙은 createdAt 필드를 추가하세요."
                    + "\n  실제 컬럼 : " + 컬럼목록());
        }

        Object value = 값읽기(col, id, title);
        assertTrue(value != null,
                () -> "글은 저장됐는데 " + col + " 이 비어 있습니다."
                        + "\n  @EnableJpaAuditing 만으로는 값이 채워지지 않습니다."
                        + "\n  Article 클래스에 @EntityListeners(AuditingEntityListener.class) 를 붙이세요."
                        + "\n  data.sql 로 미리 넣은 글은 SQL 에서 직접 넣은 값이라 정상으로 보입니다."
                        + " 화면에서 새로 등록한 글로 확인해야 합니다.");
    }

    // ---- DB 조회 ----

    private List<String> 컬럼목록() throws Exception {
        List<String> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                             + " WHERE UPPER(TABLE_NAME) = 'ARTICLE'")) {
            while (rs.next()) {
                out.add(rs.getString(1).toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    /** created_at / createdat 등 표기가 달라도 찾아낸다. */
    private String 시각컬럼() throws Exception {
        for (String c : 컬럼목록()) {
            if (c.replace("_", "").equals("CREATEDAT")) {
                return c;
            }
        }
        return null;
    }

    private Object 값읽기(String col, Long id, String title) throws Exception {
        String where = (id != null) ? "id = ?" : "title = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT \"" + col + "\" FROM ARTICLE WHERE " + where)) {
            if (id != null) {
                ps.setLong(1, id);
            } else {
                ps.setString(1, title);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    fail("등록한 글을 DB 에서 찾지 못했습니다.");
                }
                return rs.getObject(1);
            }
        }
    }
}
