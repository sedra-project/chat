package chat.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inmem")
@TestPropertySource(properties = "chat.rate.limit-per-second=1")
public class WsRateLimitTest {

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    ObjectMapper mapper;

    private String token(String username) throws Exception {
        String url = "http://localhost:" + port + "/api/v1/session";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(url, new HttpEntity<>("{\"username\":\"" + username + "\"}", h), String.class);
        JsonNode n = mapper.readTree(resp.getBody());
        return n.get("token").asText();
    }

    @Test
    void onlyFirstMessagePassesWithinOneSecond() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";
        String tA = token("alice_rl");
        String tB = token("bob_rl");

        WebSocketStompClient stompA = new WebSocketStompClient(new StandardWebSocketClient());
        stompA.setMessageConverter(new MappingJackson2MessageConverter());
        BlockingQueue<ChatMessageDto> queue = new ArrayBlockingQueue<>(10);

        StompSession a = stompA.connect(wsUrl, new WebSocketHttpHeaders(), headers(tA), new StompSessionHandlerAdapter() {
                })
                .get(3, TimeUnit.SECONDS);
        a.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((ChatMessageDto) payload);
            }
        });
        Thread.sleep(200);

        WebSocketStompClient stompB = new WebSocketStompClient(new StandardWebSocketClient());
        stompB.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession b = stompB.connect(wsUrl, new WebSocketHttpHeaders(), headers(tB), new StompSessionHandlerAdapter() {
                })
                .get(3, TimeUnit.SECONDS);

        // vider JOIN
        queue.poll(1, TimeUnit.SECONDS);

        // 2 messages quasi simultanés -> 1 seul doit passer
        b.send("/app/chat.message", "{\"content\":\"one\"}".getBytes());
        b.send("/app/chat.message", "{\"content\":\"two\"}".getBytes());

        ChatMessageDto m1 = queue.poll(3, TimeUnit.SECONDS);
        ChatMessageDto m2 = queue.poll(1, TimeUnit.SECONDS); // court

        assertNotNull(m1, "aucun message reçu");
        assertEquals("one", m1.getContent());
        assertNull(m2, "le second message ne devrait pas passer (rate limit)");

        a.disconnect();
        b.disconnect();
    }

    private StompHeaders headers(String token) {
        StompHeaders h = new StompHeaders();
        h.add("Authorization", "Bearer " + token);
        return h;
    }
}