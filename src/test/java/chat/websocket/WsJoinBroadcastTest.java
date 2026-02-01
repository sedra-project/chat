package chat.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inmem")
public class WsJoinBroadcastTest {

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
    void broadcastJoinToSubscribers() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";

        // A: token + connection + subscribe
        String tokenA = createToken("alice_A");
        WebSocketStompClient stompA = new WebSocketStompClient(new StandardWebSocketClient());
        stompA.setMessageConverter(new MappingJackson2MessageConverter());
        BlockingQueue<ChatMessageDto> queue = new ArrayBlockingQueue<>(10);

        StompHeaders connectA = new StompHeaders();
        connectA.add("Authorization", "Bearer " + tokenA);
        ListenableFuture<StompSession> fA =
                stompA.connect(wsUrl, new WebSocketHttpHeaders(), connectA, new StompSessionHandlerAdapter() {});
        StompSession sessionA = fA.get(3, TimeUnit.SECONDS);

        sessionA.subscribe("/topic/public", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return ChatMessageDto.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { queue.add((ChatMessageDto) payload); }
        });

        // petite pause pour que l'abonnement soit effectif
        Thread.sleep(200);

        // B: connexion → A doit recevoir JOIN de B
        String tokenB = createToken("bob_B");
        WebSocketStompClient stompB = new WebSocketStompClient(new StandardWebSocketClient());
        stompB.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectB = new StompHeaders();
        connectB.add("Authorization", "Bearer " + tokenB);
        StompSession sessionB =
                stompB.connect(wsUrl, new WebSocketHttpHeaders(), connectB, new StompSessionHandlerAdapter() {})
                        .get(3, TimeUnit.SECONDS);

        ChatMessageDto msg = queue.poll(3, TimeUnit.SECONDS);
        assertNotNull(msg, "A n'a pas reçu de JOIN");
        assertEquals("JOIN", msg.getType());
        //assertEquals("bob_B", msg.getSender());

        // cleanup
        sessionB.disconnect();
        sessionA.disconnect();
    }
}