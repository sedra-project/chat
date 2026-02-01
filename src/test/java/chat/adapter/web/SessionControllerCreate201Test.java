package chat.adapter.web;

import com.chat.adapter.web.ApiErrorHandler;
import com.chat.adapter.web.SessionController;
import com.chat.application.dto.SessionResult;
import com.chat.application.port.in.CreateSessionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SessionController.class)
@Import(ApiErrorHandler.class)
public class SessionControllerCreate201Test {

    @Autowired private MockMvc mvc;
    @MockBean private CreateSessionUseCase createSession;

    @Test
    void returns201WithTokenAndUsername() throws Exception {
        when(createSession.create("alice_01"))
                .thenReturn(new SessionResult("token-123", "alice_01"));

        mvc.perform(post("/api/v1/session")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"alice_01\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.username").value("alice_01"));
    }
}