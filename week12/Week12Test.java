package com.example.blog.grader.week12;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * CH10 구글 로그인 — 각자 구글 클라우드에 OAuth 클라이언트를 만들었는지 본다.
 *
 * client-id 는 비밀값이 아니다(로그인할 때 주소창에 그대로 보인다). 그래서 application.yml 에
 * 커밋하게 하고, client-secret 만 application-local.yml 에 둔다.
 * secret 은 CI 에 없으므로 실제 로그인은 확인하지 않는다(스크린샷 제출로 확인).
 *
 * 이 클래스만 시큐리티 필터를 켠다. /oauth2/authorization/google 은 필터가 처리하기 때문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Week12Test {

    /** 수업에서 등록하라고 한 redirect URI. MockMvc 는 포트 80 으로 만들어 주므로 직접 지정한다. */
    private static final String REDIRECT_URI = "http://localhost:8080/login/oauth2/code/google";

    private static final Pattern CLIENT_ID =
            Pattern.compile("^\\d+-[a-z0-9]+\\.apps\\.googleusercontent\\.com$");

    @Autowired
    MockMvc mockMvc;

    /** 앱이 구글로 보낼 때 쓰는 client_id 를 꺼낸다. 앱 내부 클래스에 의존하지 않는다. */
    private String 내_client_id() throws Exception {
        var res = mockMvc.perform(get("/oauth2/authorization/google")).andReturn().getResponse();
        String location = res.getHeader("Location");
        if (location == null || !location.startsWith("https://accounts.google.com/")) {
            fail("/oauth2/authorization/google 이 구글 로그인으로 이동하지 않습니다 (응답 " + res.getStatus()
                    + ", 이동 위치 " + location + "). application.yml 의 구글 OAuth 설정과 시큐리티 설정을 확인하세요.");
        }
        String id = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("client_id");
        return id == null ? "" : java.net.URLDecoder.decode(id, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("[CH10] application.yml 에 본인 구글 client-id 가 들어 있다 (기본값·더미 아님)")
    void clientIdCommitted() throws Exception {
        String id = 내_client_id();
        assertTrue(CLIENT_ID.matcher(id).matches(),
                "client-id 가 구글 형식(숫자-영문.apps.googleusercontent.com)이 아닙니다: '" + id
                        + "'. client-id 는 application.yml 에 커밋하고, client-secret 만 application-local.yml 에 두세요.");
    }

    @Test
    @DisplayName("[CH10] 구글 콘솔에 이 client-id 와 redirect URI(http://localhost:8080/login/oauth2/code/google)가 등록돼 있다")
    void registeredAtGoogle() throws Exception {
        String id = 내_client_id();
        String url = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&scope=email"
                + "&client_id=" + URLEncoder.encode(id, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<Void> r = http.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.discarding());

        String next = r.headers().firstValue("Location").orElse("");
        // 정상: 구글 계정 선택/로그인 화면으로 이동. 오류: /signin/oauth/error (client 없음, redirect_uri_mismatch 등)
        assertTrue(r.statusCode() == 302 && !next.contains("/signin/oauth/error") && !next.isEmpty(),
                "구글이 이 client-id 또는 redirect URI 를 거부했습니다. 구글 인증 플랫폼 > 클라이언트에서 "
                        + "client-id 가 맞는지, 승인된 리디렉션 URI 에 " + REDIRECT_URI + " 가 있는지 확인하세요.");
    }
}
