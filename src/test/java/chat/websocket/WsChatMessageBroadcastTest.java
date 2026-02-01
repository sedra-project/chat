package chat.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
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

import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inmem")
public class WsChatMessageBroadcastTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    private String createToken(String username) throws Exception {
        String url = "http://localhost:" + port + "/api/v1/session";
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity(url, new HttpEntity<>("{\"username\":\""+username+"\"}", h), String.class);
        assertEquals(201, resp.getStatusCodeValue());
        JsonNode n = mapper.readTree(resp.getBody());
        return n.get("token").asText();
    }

    @Test
    void chatMessageIsBroadcast() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";

        String tokenA = createToken("alice_S3A");
        String tokenB = createToken("bob_S3B");

        WebSocketStompClient stompA = new WebSocketStompClient(new StandardWebSocketClient());
        stompA.setMessageConverter(new MappingJackson2MessageConverter());
        BlockingQueue<ChatMessageDto> queue = new ArrayBlockingQueue<>(5);

        StompSession sessionA = stompA.connect(wsUrl, new WebSocketHttpHeaders(), headers(tokenA), new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);
        sessionA.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return ChatMessageDto.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { queue.add((ChatMessageDto) payload); }
        });
        Thread.sleep(200); // laisser le temps à l'abonnement

        WebSocketStompClient stompB = new WebSocketStompClient(new StandardWebSocketClient());
        stompB.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession sessionB = stompB.connect(wsUrl, new WebSocketHttpHeaders(), headers(tokenB), new StompSessionHandlerAdapter() {})
                .get(3, TimeUnit.SECONDS);

        // vider le JOIN de B si reçu
        queue.poll(1, TimeUnit.SECONDS);

        // B envoie un message
        sessionB.send("/app/chat.message", "{\"content\":\"Hello S3\"}".getBytes());

        ChatMessageDto msg = queue.poll(3, TimeUnit.SECONDS);
        assertNotNull(msg);
        assertEquals("CHAT", msg.getType());
        //assertEquals("bob_S3B", msg.getSender());
        assertEquals("Hello S3", msg.getContent());

        sessionB.disconnect(); sessionA.disconnect();
    }

    private StompHeaders headers(String token) {
        StompHeaders h = new StompHeaders();
        h.add("Authorization", "Bearer " + token);
        return h;
    }
}