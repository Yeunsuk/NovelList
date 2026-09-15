import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import io.github.bonigarcia.wdm.WebDriverManager;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final String RESULT_DIR = "결과";

    public static void saveBookList(List<Book> bookList, String fileName) {
        new File(RESULT_DIR).mkdirs();
        // FileWriter는 플랫폼 기본 인코딩을 씀 - 한글 깨짐 방지를 위해 UTF-8 명시
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)) {
            for (Book book : bookList) {
                writer.write(book.toString() + "\n");
            }
            log.info("북리스트가 파일에 저장되었습니다: {}", fileName);
        } catch (IOException e) {
            log.error("파일 저장 중 오류가 발생했습니다: {}", e.getMessage());
        }
    }

    // 파일명에 못 쓰는 문자 제거 + 길이 제한
    static String sanitizeFileName(String raw) {
        String cleaned = raw.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        return cleaned.isEmpty() ? "검색결과" : cleaned;
    }

    static String buildFileName(String platform, SearchQuery query) {
        String keyword;
        if (!query.getTitle().isEmpty()) {
            keyword = query.getTitle();
        } else if (!query.getAuthor().isEmpty()) {
            keyword = query.getAuthor();
        } else {
            keyword = String.join(",", query.getTags());
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return RESULT_DIR + "/" + platform + "_" + sanitizeFileName(keyword) + "_" + timestamp + ".txt";
    }

    public static void main(String[] args) {
        // 콘솔 인코딩이 UTF-8이 아닌 환경(한국어 Windows 등)에서 로그 한글 깨짐 방지
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        // WebDriverManager를 사용하여 ChromeDriver 자동 다운로드 및 설정
        if (System.getProperty("webdriver.chrome.driver") == null) {
            WebDriverManager.chromedriver().setup();
        }

        //UI 관련 코드: 검색 버튼 클릭 시 바로 크롤링 실행 (폴링 없음)
        UI ui = new UI();
        ui.First(query -> {
            ProgressWindow progress = new ProgressWindow();

            if (query.getPlatforms().contains("Series")) {
                Execution ex1 = new Execution();
                List<Book> naver = ex1.Start("naver", query, progress::update);
                naver.sort(Book.Sort);
                saveBookList(naver, buildFileName("naver", query));
                SwingUtilities.invokeLater(() -> ui.End(naver, "Naver"));
            }

            if (query.getPlatforms().contains("Kakao")) {
                Execution ex2 = new Execution();
                List<Book> kakao = ex2.Start("kakao", query, progress::update);
                kakao.sort(Book.Sort);
                saveBookList(kakao, buildFileName("kakao", query));
                SwingUtilities.invokeLater(() -> ui.End(kakao, "Kakao"));
            }

            if (query.getPlatforms().contains("Pia")) {
                Execution ex3 = new Execution();
                List<Book> pia = ex3.Start("pia", query, progress::update);
                pia.sort(Book.Sort);
                saveBookList(pia, buildFileName("pia", query));
                SwingUtilities.invokeLater(() -> ui.End(pia, "Pia"));
            }

            progress.update("전체 검색 완료");
        });
    }
}
