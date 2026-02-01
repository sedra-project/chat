package chat.domain;

import com.chat.adapter.persistence.inmemory.InMemorySessionTokenRepository;
import com.chat.adapter.persistence.common.UsernameReservation;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UsernameReservationPort;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("inmem")
class InmemProfileWiringTest {

    @Autowired
    UsernameReservationPort reservationPort;
    @Autowired
    SessionTokenRepositoryPort tokenPort;

    @Test
    void shouldWireInMemoryAdapters() {
        assertEquals(UsernameReservation.class, AopUtils.getTargetClass(reservationPort));
        assertEquals(InMemorySessionTokenRepository.class, AopUtils.getTargetClass(tokenPort));
    }
}