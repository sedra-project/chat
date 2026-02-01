package chat.application.usecase;

import com.chat.application.dto.SessionResult;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UsernameReservationPort;
import com.chat.application.usecase.CreateSessionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CreateSessionServiceSuccessTest {

    @Test
    void createsSessionWhenUsernameAvailable() {
        SessionTokenRepositoryPort tokens = mock(SessionTokenRepositoryPort.class);
        UsernameReservationPort reservations = mock(UsernameReservationPort.class);

        when(reservations.tryReserve("alice_01")).thenReturn(true);
        when(tokens.createToken("alice_01")).thenReturn("token-123");

        CreateSessionService service = new CreateSessionService(tokens, reservations);
        SessionResult r = service.create("alice_01");

        assertEquals("token-123", r.getToken());
        assertEquals("alice_01", r.getUsername());
        verify(reservations).tryReserve("alice_01");
        verify(tokens).createToken("alice_01");
        verifyNoMoreInteractions(tokens, reservations);
    }
}