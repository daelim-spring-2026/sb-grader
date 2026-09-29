package com.example.blog.grader.week06;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 학생이 직접 테스트 코드를 작성했는지 본다.
 *
 * 소스를 문자열로 뒤지지 않는다. 컴파일된 테스트 클래스를 읽어 @Test 가 붙은
 * 메서드를 세므로 메서드 이름이나 주석을 어떻게 쓰든 영향이 없다.
 * 채점기 자신(grader 패키지)은 당연히 제외한다.
 *
 * 작성 여부만 본다. 학생이 만든 테스트의 통과 여부는 점수에 넣지 않는다.
 */
class Week06Test {

    private static final String GRADER_PKG = "com.example.blog.grader.";

    private static List<Class<?>> 학생테스트클래스;
    private static int 학생테스트메서드;
    private static String 스캔오류;

    private static void 스캔() {
        if (학생테스트클래스 != null || 스캔오류 != null) {
            return;
        }
        List<Class<?>> classes = new ArrayList<>();
        int methods = 0;
        try {
            Path root = Paths.get(Week06Test.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> files = walk.filter(p -> p.toString().endsWith(".class")).toList();
                for (Path f : files) {
                    String name = root.relativize(f).toString()
                            .replace(java.io.File.separatorChar, '.')
                            .replaceAll("\\.class$", "");
                    if (name.startsWith(GRADER_PKG)) {
                        continue;
                    }
                    Class<?> c;
                    try {
                        c = Class.forName(name, false, Week06Test.class.getClassLoader());
                    } catch (Throwable t) {
                        continue;   // 로딩 못 하는 클래스는 센서에서 뺀다
                    }
                    int n = 0;
                    for (Method m : c.getDeclaredMethods()) {
                        for (Annotation a : m.getAnnotations()) {
                            String an = a.annotationType().getName();
                            if (an.equals("org.junit.jupiter.api.Test")
                                    || an.equals("org.junit.jupiter.params.ParameterizedTest")
                                    || an.equals("org.junit.jupiter.api.RepeatedTest")) {
                                n++;
                                break;
                            }
                        }
                    }
                    if (n > 0) {
                        classes.add(c);
                        methods += n;
                    }
                }
            }
        } catch (IOException | RuntimeException | java.net.URISyntaxException e) {
            스캔오류 = e.getClass().getSimpleName() + " : " + e.getMessage();
            return;
        }
        학생테스트클래스 = classes;
        학생테스트메서드 = methods;
    }

    private void 확인() {
        스캔();
        if (스캔오류 != null) {
            fail("테스트 클래스를 훑지 못했습니다. 교수에게 알려주세요.\n  " + 스캔오류);
        }
    }

    private String 목록() {
        return 학생테스트클래스.isEmpty()
                ? "(없음)"
                : 학생테스트클래스.stream().map(Class::getSimpleName).toList().toString();
    }

    @Test
    @DisplayName("[테스트 코드] 본인이 작성한 테스트 클래스가 있다")
    void 작성했는가() {
        확인();
        assertTrue(!학생테스트클래스.isEmpty(),
                () -> "@Test 가 붙은 테스트 클래스를 찾지 못했습니다."
                        + "\n  src/test/java/com/example/blog/ 아래에 테스트를 만드세요."
                        + "\n  CH04 와 CH05 자료의 BlogControllerTest 를 참고하면 됩니다."
                        + "\n  채점기가 넣는 테스트는 세지 않습니다. 본인이 쓴 것만 셉니다.");
    }

    @Test
    @DisplayName("[테스트 코드] 블로그 API 테스트를 5개 이상 작성했다")
    void 다섯개이상() {
        확인();
        assertTrue(학생테스트메서드 >= 5,
                () -> "본인이 쓴 @Test 메서드가 " + 학생테스트메서드 + " 개입니다. 5개 이상 필요합니다."
                        + "\n  CH05 자료에 등록 · 목록조회 · 단건조회 · 수정 · 삭제 다섯 개가 있습니다."
                        + "\n  찾은 테스트 클래스 : " + 목록());
    }
}
