import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainFileNameTest {

    @Test
    void sanitizeFileNameReplacesIllegalCharacters() {
        String result = Main.sanitizeFileName("제목: <이상한/파일*이름?>");
        assertFalse(result.contains(":"));
        assertFalse(result.contains("<"));
        assertFalse(result.contains("/"));
        assertFalse(result.contains("*"));
        assertFalse(result.contains("?"));
        assertFalse(result.contains(">"));
    }

    @Test
    void sanitizeFileNameFallsBackWhenEmpty() {
        assertEquals("검색결과", Main.sanitizeFileName(""));
    }

    @Test
    void sanitizeFileNameTruncatesLongInput() {
        String longTitle = "가".repeat(200);
        String result = Main.sanitizeFileName(longTitle);
        assertTrue(result.length() <= 50);
    }

    @Test
    void buildFileNameUsesTitleWhenPresent() {
        SearchQuery query = new SearchQuery("판타지 소설", "", List.of(), "합집합", "Series ");
        String fileName = Main.buildFileName("naver", query);

        assertTrue(fileName.startsWith("결과/naver_"));
        assertTrue(fileName.contains("판타지 소설") || fileName.contains("판타지_소설"));
        assertTrue(fileName.endsWith(".txt"));
    }

    @Test
    void buildFileNameFallsBackToTagsWhenNoTitleOrAuthor() {
        SearchQuery query = new SearchQuery("", "", List.of("판타지", "무협"), "합집합", "Series ");
        String fileName = Main.buildFileName("naver", query);

        assertTrue(fileName.contains("판타지"));
    }
}
