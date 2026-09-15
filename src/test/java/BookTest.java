import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookTest {

    private Book book(String title, int score) {
        return new Book(title, score, new ArrayList<>(), "", "", "", "naver");
    }

    @Test
    void sortOrdersByScoreDescending() {
        List<Book> books = new ArrayList<>(List.of(
                book("low", 10),
                book("high", 90),
                book("mid", 50)
        ));

        Collections.sort(books, Book.Sort);

        assertEquals("high", books.get(0).getTitle());
        assertEquals("mid", books.get(1).getTitle());
        assertEquals("low", books.get(2).getTitle());
    }

    @Test
    void toStringFillsInDefaultsForEmptyFields() {
        Book book = new Book("", 0, new ArrayList<>(), "", "", "", "");
        String result = book.toString();

        assertEquals(true, result.contains("없음"));
        assertEquals(true, result.contains("미정"));
    }
}
