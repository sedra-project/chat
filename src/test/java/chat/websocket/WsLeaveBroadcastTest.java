package chat.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inmem")
public class WsLeaveBroadcastTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper mapper;

    private String createToken(String username) throws Exception {
        String url = "http://localhost:" + port + "/api/v1/session";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + username + "\"}";
        ResponseEntity<String> resp = rest.postForEntity(url, new HttpEntity<>(body, h), String.class);
        assertEquals(201, resp.getStatusCodeValue());
        JsonNode node = mapper.readTree(resp.getBody());
        return node.get("token").asText();
    }

    @Test
    void broadcastLeaveOnDisconnect() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";

        // A connecté et abonné
        String tokenA = createToken("alice_A2");
        WebSocketStompClient stompA = new WebSocketStompClient(new StandardWebSocketClient());
        stompA.setMessageConverter(new MappingJackson2MessageConverter());
        BlockingQueue<ChatMessageDto> queue = new ArrayBlockingQueue<>(10);

        StompSession sessionA =
                stompA.connect(wsUrl, new WebSocketHttpHeaders(), connectHeaders(tokenA), new StompSessionHandlerAdapter() {})
                        .get(3, TimeUnit.SECONDS);

        sessionA.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return ChatMessageDto.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { queue.add((ChatMessageDto) payload); }
        });
        Thread.sleep(200);

        // B connecté puis déconnecté
        String tokenB = createToken("bob_B2");
        WebSocketStompClient stompB = new WebSocketStompClient(new StandardWebSocketClient());
        stompB.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession sessionB =
                stompB.connect(wsUrl, new WebSocketHttpHeaders(), connectHeaders(tokenB), new StompSessionHandlerAdapter() {})
                        .get(3, TimeUnit.SECONDS);

        // vider éventuellement le JOIN de B
        queue.poll(2, TimeUnit.SECONDS);

        // Déconnexion de B → A doit recevoir LEAVE
        sessionB.disconnect();

        ChatMessageDto leave = queue.poll(3, TimeUnit.SECONDS);
        assertNotNull(leave, "A n'a pas reçu de LEAVE");
        assertEquals("LEAVE", leave.getType());
        //assertEquals("bob_B2", leave.getSender());

        // cleanup
        sessionA.disconnect();
    }

    private StompHeaders connectHeaders(String token) {
        StompHeaders h = new StompHeaders();
        h.add("Authorization", "Bearer " + token);
        return h;
    }
}