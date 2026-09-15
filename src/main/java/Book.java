import java.util.Comparator;
import java.util.List;

// 크롤링 결과 한 건 (소설 정보). 검색 조건은 SearchQuery 참고.
public class Book {
    private String title; // 책 제목
    private int score; // 책 점수
    private List<String> tags; // 태그 리스트
    private String author; // 작가
    private String link; // 책 링크
    private String description; // 책 설명(줄거리)
    private String platform; // 플랫폼

    // 생성자
    public Book(String title, int score, List<String> tags, String author, String link, String description, String platform) {
        this.title = title;
        this.score = score;
        this.tags = tags;
        this.author = author;
        this.link = link;
        this.description = description;
        this.platform = platform;
    }


    // Getter와 Setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatform() {
        return platform;
    }

    // 출력 형식 정의
    @Override
    public String toString() {
        return "Book{" +
                "title='" + (title.isEmpty() ? "없음" : title) + '\'' +
                ", score=" + score +
                ", tags=" + (tags.isEmpty() ? "없음" : tags) +
                ", author='" + (author.isEmpty() ? "미정" : author) + '\'' +
                ", link='" + (link.isEmpty() ? "없음" : link) + '\'' +
                ", description='" + (description.isEmpty() ? "미정" : description) + '\'' +
                ", platform='" + (platform.isEmpty() ? "없음" : platform) + '\'' +
                '}';
    }

    // 점수 기준 내림차순 Comparator
    public static final Comparator<Book> Sort = new Comparator<Book>() {
        @Override
        public int compare(Book b1, Book b2) {
            return Integer.compare(b2.score, b1.score); // 내림차순 정렬
        }
    };
}
