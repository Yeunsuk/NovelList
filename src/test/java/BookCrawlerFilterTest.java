import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookCrawlerFilterTest {

    private BookCrawler crawlerWith(String tagOperation, List<String> queryTags) {
        SearchQuery query = new SearchQuery("", "", queryTags, tagOperation, "");
        return new BookCrawler("url", "naver", new ArrayList<>(), query);
    }

    @Test
    void unionKeepsBookWhenAnyQueryTagPresent() {
        BookCrawler crawler = crawlerWith("합집합", List.of("판타지", "무협"));

        assertFalse(crawler.isFilteredOut(List.of("판타지", "로맨스"))); // 하나라도 겹치면 통과
        assertTrue(crawler.isFilteredOut(List.of("로맨스", "완결"))); // 둘 다 없음 -> 걸러짐
    }

    @Test
    void intersectionKeepsBookOnlyWhenAllQueryTagsPresent() {
        BookCrawler crawler = crawlerWith("교집합", List.of("판타지", "무협"));

        assertFalse(crawler.isFilteredOut(List.of("판타지", "무협", "완결")));
        assertTrue(crawler.isFilteredOut(List.of("판타지"))); // 무협 빠짐 -> 걸러짐
    }

    @Test
    void unknownOperationNeverFiltersOut() {
        BookCrawler crawler = crawlerWith("", List.of("판타지"));

        assertFalse(crawler.isFilteredOut(List.of("아무거나")));
    }
}
