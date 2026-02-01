package chat.domain;

import com.chat.domain.model.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UsernameValidTest {
    @Test
    void acceptsValidUsername() {
        Username u = Username.of("Alice_01");
        assertEquals("Alice_01", u.value());
    }
}