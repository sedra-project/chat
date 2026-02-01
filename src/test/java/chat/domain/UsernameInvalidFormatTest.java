package chat.domain;

import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UsernameInvalidFormatTest {
    @Test
    void rejectsInvalidChars() {
        assertThrows(DomainException.class, () -> Username.of("a!"));
    }
}