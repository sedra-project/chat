package chat.application.usecase;

import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UsernameReservationPort;
import com.chat.application.usecase.CreateSessionService;
import com.chat.domain.exceptions.DuplicateUsernameException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class CreateSessionServiceDuplicateTest {

    @Test
    void throwsWhenUsernameAlreadyReserved() {
        SessionTokenRepositoryPort tokens = mock(SessionTokenRepositoryPort.class);
        UsernameReservationPort reservations = mock(UsernameReservationPort.class);

        when(reservations.tryReserve("alice_01")).thenReturn(false);

        CreateSessionService service = new CreateSessionService(tokens, reservations);
        assertThrows(DuplicateUsernameException.class, () -> service.create("alice_01"));

        verify(reservations).tryReserve("alice_01");
        verifyNoMoreInteractions(tokens, reservations);
    }
}