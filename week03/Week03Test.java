package com.example.blog.grader.week03;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 3주차 채점 — CH05 글 등록 API
 *
 * HTTP 로만 검증한다. 학생의 패키지명·클래스명·구현 방식에 의존하지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Week03Test {

    @Autowired
    MockMvc mockMvc;

    @DisplayName("[3주차] POST /api/articles 로 글을 등록하면 201 과 등록된 내용을 돌려준다")
    @Test
    void addArticle() throws Exception {
        String title = "t" + System.nanoTime();
        String body = "{\"title\":\"" + title + "\",\"content\":\"content\"}";

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value("content"));
    }
}
