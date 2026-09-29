package com.example.blog.grader.week06;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.SpringAsmInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 학생이 직접 테스트 코드를 작성했는지 본다.
 *
 * @Test 만 붙이고 본문이 빈 메서드는 세지 않는다. 컴파일된 바이트코드를 읽어
 * 검증 호출(assertThat, assertEquals, andExpect 등)이 실제로 들어 있는 테스트만 센다.
 * 소스를 문자열로 뒤지지 않으므로 주석이나 이름과 무관하다.
 *
 * 테스트가 같은 클래스의 도우미 메서드를 불러 그 안에서 검증해도 인정한다.
 * 채점기 자신(grader 패키지)은 제외한다.
 *
 * 작성 여부만 본다. 학생 테스트의 통과 여부는 점수에 넣지 않는다.
 */
class Week06Test {

    private static final String GRADER_PKG = "com/example/blog/grader/";

    private static final Set<String> TEST_ANN = Set.of(
            "Lorg/junit/jupiter/api/Test;",
            "Lorg/junit/jupiter/params/ParameterizedTest;",
            "Lorg/junit/jupiter/api/RepeatedTest;");

    /** 검증으로 인정하는 호출. owner 는 내부 이름(슬래시 구분). */
    private static boolean 검증호출(String owner, String name) {
        return owner.startsWith("org/junit/jupiter/api/Assertions")
                || owner.startsWith("org/junit/Assert")
                || owner.startsWith("org/assertj/")
                || owner.startsWith("org/hamcrest/MatcherAssert")
                || (owner.equals("org/springframework/test/web/servlet/ResultActions") && name.equals("andExpect"))
                || (owner.startsWith("org/mockito/") && name.equals("verify"));
    }

    private static final class 메서드 {
        String 이름;
        boolean 테스트;
        boolean 검증;
        final Set<String> 내부호출 = new HashSet<>();
    }

    private static final class 결과 {
        final List<String> 검증있음 = new ArrayList<>();    // "클래스.메서드"
        final List<String> 비어있음 = new ArrayList<>();
        final Set<String> 클래스 = new LinkedHashSet<>();
    }

    private static 결과 스캔결과;
    private static String 스캔오류;

    private static void 스캔() {
        if (스캔결과 != null || 스캔오류 != null) {
            return;
        }
        결과 r = new 결과();
        try {
            Path root = Paths.get(Week06Test.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            List<Path> files;
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(p -> p.toString().endsWith(".class")).toList();
            }
            for (Path f : files) {
                ClassReader cr = new ClassReader(Files.readAllBytes(f));
                String cls = cr.getClassName();
                if (cls.startsWith(GRADER_PKG)) {
                    continue;
                }
                Map<String, 메서드> ms = new LinkedHashMap<>();
                cr.accept(new ClassVisitor(SpringAsmInfo.ASM_VERSION) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc,
                                                     String sig, String[] ex) {
                        메서드 m = new 메서드();
                        m.이름 = name;
                        ms.put(name + desc, m);
                        return new MethodVisitor(SpringAsmInfo.ASM_VERSION) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String d, boolean visible) {
                                if (TEST_ANN.contains(d)) {
                                    m.테스트 = true;
                                }
                                return null;
                            }

                            @Override
                            public void visitMethodInsn(int op, String owner, String n,
                                                        String d, boolean itf) {
                                if (검증호출(owner, n)) {
                                    m.검증 = true;
                                }
                                if (owner.equals(cls)) {
                                    m.내부호출.add(n + d);
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                String simple = cls.substring(cls.lastIndexOf('/') + 1);
                for (메서드 m : ms.values()) {
                    if (!m.테스트) {
                        continue;
                    }
                    r.클래스.add(simple);
                    String label = simple + "." + m.이름;
                    if (검증하는가(m, ms, new HashSet<>())) {
                        r.검증있음.add(label);
                    } else {
                        r.비어있음.add(label);
                    }
                }
            }
        } catch (Exception e) {
            스캔오류 = e.getClass().getSimpleName() + " : " + e.getMessage();
            return;
        }
        스캔결과 = r;
    }

    /** 본인 또는 같은 클래스에서 부른 도우미 메서드 안에 검증 호출이 있는가. */
    private static boolean 검증하는가(메서드 m, Map<String, 메서드> ms, Set<메서드> 방문) {
        if (!방문.add(m)) {
            return false;
        }
        if (m.검증) {
            return true;
        }
        for (String key : m.내부호출) {
            메서드 callee = ms.get(key);
            if (callee != null && 검증하는가(callee, ms, 방문)) {
                return true;
            }
        }
        return false;
    }

    private 결과 확인() {
        스캔();
        if (스캔오류 != null) {
            fail("테스트 클래스를 훑지 못했습니다. 교수에게 알려주세요.\n  " + 스캔오류);
        }
        return 스캔결과;
    }

    private static String 앞몇개(List<String> xs) {
        if (xs.isEmpty()) {
            return "(없음)";
        }
        List<String> head = xs.subList(0, Math.min(6, xs.size()));
        return head + (xs.size() > 6 ? " 외 " + (xs.size() - 6) + "개" : "");
    }

    @Test
    @DisplayName("[테스트 코드] 본인이 작성한 테스트 클래스가 있다")
    void 작성했는가() {
        결과 r = 확인();
        assertTrue(!r.검증있음.isEmpty(), () -> r.클래스.isEmpty()
                ? "@Test 가 붙은 테스트를 찾지 못했습니다."
                        + "\n  src/test/java/com/example/blog/ 아래에 테스트를 만드세요."
                        + "\n  CH04 와 CH05 자료의 BlogControllerTest 를 참고하면 됩니다."
                : "@Test 는 있지만 안에서 아무것도 검증하지 않습니다."
                        + "\n  assertThat(...), assertEquals(...), andExpect(...) 같은 검증이 있어야 테스트입니다."
                        + "\n  비어 있는 테스트 : " + 앞몇개(r.비어있음));
    }

    @Test
    @DisplayName("[테스트 코드] 블로그 API 테스트를 5개 이상 작성했다")
    void 다섯개이상() {
        결과 r = 확인();
        assertTrue(r.검증있음.size() >= 5,
                () -> "검증이 들어 있는 테스트가 " + r.검증있음.size() + " 개입니다. 5개 이상 필요합니다."
                        + "\n  CH05 자료에 등록 · 목록조회 · 단건조회 · 수정 · 삭제 다섯 개가 있습니다."
                        + (r.비어있음.isEmpty() ? ""
                        : "\n  아래는 @Test 만 있고 검증이 없어 세지 않았습니다 : " + 앞몇개(r.비어있음)));
    }
}
