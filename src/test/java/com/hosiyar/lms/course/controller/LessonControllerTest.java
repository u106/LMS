package com.hosiyar.lms.course.controller;

import com.hosiyar.lms.course.service.LessonService;
import com.hosiyar.lms.user.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the security gate around lessons. We're testing the gate -
 * routing, roles - not the business logic behind it, which is covered by
 * LessonServiceTest.
 *
 * MockMvc is built manually with springSecurity() applied. The auto-configured
 * MockMvc in a @WebMvcTest slice does not reliably run a custom SecurityFilter-
 * Chain against multipart POST requests, so security would be skipped and every
 * request would return 200. Building it by hand from the web context wires the
 * real filter chain in, which is exactly what these tests are checking.
 *
 * Maps to docs/prd/module-02-course-management.md US-6 and US-7.
 */
@WebMvcTest(LessonController.class)
@Import({
        com.hosiyar.lms.user.config.SecurityConfig.class,
        com.hosiyar.lms.common.config.CorsConfig.class,
        com.hosiyar.lms.user.security.JwtAuthenticationEntryPoint.class,
        com.hosiyar.lms.user.security.RestAccessDeniedHandler.class
})
class LessonControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UUID courseId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();

    @BeforeEach
    void setUp(WebApplicationContext context) throws Exception {
        // The real JwtAuthenticationFilter is mocked. A mocked OncePerRequest-
        // Filter runs its real doFilter(), which delegates to doFilterInternal()
        // - and the mock's doFilterInternal does nothing, so it never calls
        // chain.doFilter() and the request halts inside the filter (every test
        // would see an empty 200). Stub doFilterInternal to pass straight
        // through. These tests exercise the authorization rules, not token
        // parsing, which is covered separately at the service layer.
        org.mockito.Mockito.doAnswer(invocation -> {
                    jakarta.servlet.http.HttpServletRequest req = invocation.getArgument(0);
                    jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
                    jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                    chain.doFilter(req, res);
                    return null;
                }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("uploading a file without a token gives 401")
    void uploadWithoutTokenIsUnauthorized() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[16]);

        mockMvc.perform(multipart("/api/v1/courses/{c}/lessons/{l}/file", courseId, lessonId)
                        .file(file))
                .andExpect(status().isUnauthorized());

        verify(lessonService, never()).attachFile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("a student uploading a file gives 403 - wrong role")
    @WithMockUser(roles = "STUDENT")
    void uploadAsStudentIsForbidden() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[16]);

        mockMvc.perform(multipart("/api/v1/courses/{c}/lessons/{l}/file", courseId, lessonId)
                        .file(file))
                .andExpect(status().isForbidden());

        verify(lessonService, never()).attachFile(any(), any(), any(), any());
    }

    // Note: the instructor happy-path isn't slice-tested here. @WithMockUser
    // doesn't populate a real AuthenticatedUser principal, so the controller's
    // caller.getId() would NPE - a test artifact, not a real bug. The upload
    // success path is covered end to end in LessonServiceTest instead, which
    // is where the actual logic lives.
}