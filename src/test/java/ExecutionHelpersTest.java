import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExecutionHelpersTest {

    @Test
    void naverGenreCodeMapsKnownGenres() {
        assertEquals("201", Execution.naverGenreCode("로맨스"));
        assertEquals("202", Execution.naverGenreCode("판타지"));
        assertEquals("206", Execution.naverGenreCode("무협"));
        assertEquals("208", Execution.naverGenreCode("현대판타지"));
        assertEquals("208", Execution.naverGenreCode("현판"));
        assertEquals("207", Execution.naverGenreCode("로판"));
        assertEquals("203", Execution.naverGenreCode("미스터리"));
        assertEquals("205", Execution.naverGenreCode("라이트노벨"));
        assertEquals("209", Execution.naverGenreCode("BL"));
    }

    @Test
    void naverGenreCodeReturnsNullForUnknownTag() {
        assertNull(Execution.naverGenreCode("아무말이나입력한태그"));
    }

    @Test
    void dedupeByLinkKeepsFirstOccurrenceOnly() {
        Book a = new Book("A", 10, new ArrayList<>(), "", "link1", "", "naver");
        Book b = new Book("A중복", 5, new ArrayList<>(), "", "link1", "", "naver");
        Book c = new Book("B", 20, new ArrayList<>(), "", "link2", "", "naver");

        List<Book> result = Execution.dedupeByLink(List.of(a, b, c));

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getTitle());
        assertEquals("B", result.get(1).getTitle());
    }
}
