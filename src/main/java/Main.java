// 진행률 추가
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.Comparator;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class Main {
    public static void saveBookList(List<Book> bookList, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            for (Book book : bookList) {
                writer.write(book.toString() + "\n");
            }
            System.out.println("북리스트가 파일에 저장되었습니다.");
        } catch (IOException e) {
            System.out.println("파일 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    public static void main(String[] args) {

        
        /*// css찾기
        WebDriver driver = new ChromeDriver();
        // 웹사이트로 이동
        driver.get("https://novelpia.com//novel/337811");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> divElements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.epnew-wrapper.s_inv > div.epnew-novel-info > div.ep-info-line.epnew-novel-title")));
        
        for (WebElement div : divElements) {
            System.out.println("div 클래스 이름: " + div.getDomAttribute("class"));
            System.out.println("div 텍스트: " + div.getText().replaceAll("\\s+", " ").trim());
            //System.out.println(div.getDomAttribute("href"));
        }
        driver.quit(); */
 
         
        // WebDriverManager를 사용하여 ChromeDriver 자동 다운로드 및 설정
        if (System.getProperty("webdriver.chrome.driver") == null) {
            WebDriverManager.chromedriver().setup();
        }

        //UI 관련 코드
        UI ui = new UI();
        ui.First();

        Book previousInput = null;

        while (true) {
            try {
                // 1초 대기
                Thread.sleep(1000);
                Book input = ui.getBook();

                if (input != null) {
                    // 이전 input과 현재 input이 같으면 1초 대기
                    if (!input.equals(previousInput)) {
                        // 이전 값 갱신
                        previousInput = input;

                        if (input.getPlatform().contains("Series")) {
                            Execution ex1 = new Execution();
                            List<Book> naver = ex1.Start("naver", input);
                            //ui.End(naver, "Naver");
                            naver.sort(Book.Sort);
                            saveBookList(naver, input.toString());
                        }
                
                        if (input.getPlatform().contains("Kakao")) {
                            Execution ex2 = new Execution();
                            List<Book> kakao = ex2.Start("kakao", input);
                            //ui.End(kakao, "Kakao");
                            kakao.sort(Book.Sort);
                            saveBookList(kakao, input.toString());
                        }
                
                        if (input.getPlatform().contains("Pia")) {
                            Execution ex3 = new Execution();
                            List<Book> Pia = ex3.Start("pia", input);
                            //ui.End(Pia, "Pia");
                            Pia.sort(Book.Sort);
                            saveBookList(Pia, input.toString());
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}



class Book {
    private String title; // 책 제목
    private int score; // 책 점수
    private List<String> tags; // 태그 리스트
    private String author; // 작가
    private String link; // 책 링크
    private String description; // 책 설명
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