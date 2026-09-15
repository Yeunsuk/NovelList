import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelectorConfigTest {

    @Test
    void loadsBundledSelectorForKnownKey() {
        assertEquals("a.page-link", SelectorConfig.get("pia.page"));
    }

    @Test
    void throwsForUnknownKey() {
        assertThrows(IllegalStateException.class, () -> SelectorConfig.get("no.such.key"));
    }
}
