package chat.domain;


import com.chat.adapter.persistence.redis.RedisSessionTokenRepository;
import com.chat.adapter.persistence.redis.RedisUsernameReservation;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UsernameReservationPort;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("redis")
class RedisProfileWiringTest {

    @Autowired
    UsernameReservationPort reservationPort;
    @Autowired
    SessionTokenRepositoryPort tokenPort;

    @Test
    void shouldWireRedisAdapters() {
        assertEquals(RedisUsernameReservation.class, AopUtils.getTargetClass(reservationPort));
        assertEquals(RedisSessionTokenRepository.class, AopUtils.getTargetClass(tokenPort));
    }
}
