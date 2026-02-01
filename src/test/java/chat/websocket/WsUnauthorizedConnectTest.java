package chat.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("inmem")
public class WsUnauthorizedConnectTest {

    @LocalServerPort
    int port;

    @Test
    void rejectConnectionWithoutToken() {
        String wsUrl = "ws://localhost:" + port + "/ws";

        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders(); // pas d’Authorization
        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();

        assertThrows(ExecutionException.class, () ->
                stomp.connect(wsUrl, wsHeaders, connectHeaders, new StompSessionHandlerAdapter() {})
                        .get(3, TimeUnit.SECONDS)
        );
    }
}