import java.util.List;

// UI에서 입력받은 검색 조건. 크롤링 결과(Book)와 구분되는 별도 모델.
public class SearchQuery {
    private String title;
    private String author;
    private List<String> tags;
    private String tagOperation; // "합집합" 또는 "교집합"
    private String platforms; // 공백으로 구분된 선택 플랫폼: "Series KakaoPage NovelPia "

    public SearchQuery(String title, String author, List<String> tags, String tagOperation, String platforms) {
        this.title = title;
        this.author = author;
        this.tags = tags;
        this.tagOperation = tagOperation;
        this.platforms = platforms;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getTagOperation() {
        return tagOperation;
    }

    public String getPlatforms() {
        return platforms;
    }
}
