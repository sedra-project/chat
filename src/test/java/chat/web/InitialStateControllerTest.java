package chat.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inmem")
public class InitialStateControllerTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    private String createToken(String username) {
        String url = "http://localhost:" + port + "/api/v1/session";
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(url, new HttpEntity<>("{\"username\":\""+username+"\"}", h), String.class);
        assertEquals(201, resp.getStatusCodeValue());
        return read(resp.getBody()).get("token").asText();
    }

    @Test
    void returnsUsersAndRecentMessages() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";
        String tA = createToken("alice_state");
        String tB = createToken("bob_state");

        // Connect B et envoie un message
        WebSocketStompClient stompB = new WebSocketStompClient(new StandardWebSocketClient());
        stompB.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession b = stompB.connect(wsUrl, new WebSocketHttpHeaders(), headers(tB), new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);
        // envoyer un message pour remplir l'historique
        b.send("/app/chat.message", "{\"content\":\"hello-history\"}".getBytes());
        // petit délai
        Thread.sleep(200);

        // GET /state avec le token A
        HttpHeaders h = new HttpHeaders(); h.add("Authorization", "Bearer " + tA);
        ResponseEntity<String> resp = rest.exchange("http://localhost:" + port + "/api/v1/state",
                HttpMethod.GET, new HttpEntity<>(h), String.class);

        assertEquals(200, resp.getStatusCodeValue());
        JsonNode node = read(resp.getBody());
        assertTrue(node.get("users").isArray());
        assertTrue(node.get("messages").isArray());
        assertTrue(node.get("users").toString().contains("bob_state"));
        assertEquals("hello-history", node.get("messages").get(0).get("content").asText());

        b.disconnect();
    }

    private StompHeaders headers(String token) { StompHeaders h = new StompHeaders(); h.add("Authorization","Bearer " + token); return h; }
    private JsonNode read(String s) { try { return mapper.readTree(s); } catch (Exception e) { throw new RuntimeException(e); } }
}