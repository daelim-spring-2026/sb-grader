package com.example.blog.grader.week04;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4주차 채점 — CH05 조회 · 수정 · 삭제 API
 *
 * 각 테스트는 자기 데이터를 직접 만들고 고유 제목으로 구분한다.
 * 따라서 DB 에 남아 있는 다른 데이터의 영향을 받지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Week04Test {

    @Autowired
    MockMvc mockMvc;

    /** 글 하나를 등록하고 id 를 돌려준다. 등록 API 가 안 되면 여기서 먼저 실패한다. */
    private long create(String title, String content) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}";
        String res = mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(res);
        assertThat(m.find())
                .withFailMessage("글 등록 응답에 id 가 없다. 응답 = %s", res)
                .isTrue();
        return Long.parseLong(m.group(1));
    }

    private String listBody() throws Exception {
        return mockMvc.perform(get("/api/articles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @DisplayName("[4주차] GET /api/articles 목록에 방금 등록한 글이 들어 있다")
    @Test
    void findAllArticles() throws Exception {
        String title = "list" + System.nanoTime();
        create(title, "content");
        assertThat(listBody()).contains(title);
    }

    @DisplayName("[4주차] GET /api/articles/{id} 로 글 하나를 조회할 수 있다")
    @Test
    void findArticle() throws Exception {
        String title = "one" + System.nanoTime();
        long id = create(title, "content");

        mockMvc.perform(get("/api/articles/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value("content"));
    }

    @DisplayName("[4주차] PUT /api/articles/{id} 로 수정하면 조회 결과가 바뀐다")
    @Test
    void updateArticle() throws Exception {
        long id = create("old" + System.nanoTime(), "old content");
        String newTitle = "new" + System.nanoTime();
        String body = "{\"title\":\"" + newTitle + "\",\"content\":\"new content\"}";

        mockMvc.perform(put("/api/articles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/articles/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(newTitle))
                .andExpect(jsonPath("$.content").value("new content"));
    }

    @DisplayName("[4주차] DELETE /api/articles/{id} 로 삭제하면 목록에서 사라진다")
    @Test
    void deleteArticle() throws Exception {
        String title = "del" + System.nanoTime();
        long id = create(title, "content");
        assertThat(listBody()).contains(title);

        mockMvc.perform(delete("/api/articles/{id}", id))
                .andExpect(status().isOk());

        assertThat(listBody())
                .withFailMessage("삭제했는데 목록에 아직 남아 있다")
                .doesNotContain(title);
    }
}
