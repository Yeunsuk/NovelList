import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//입력예시 : Book{title='없음', score=0, tags=[현대판타지, 무협, fgdf, df], author='미정', link='없음', description='합집합', platform='Series KakaoPage '}
// 카카오 스크롤롤
class BookCrawler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BookCrawler.class);

    public String navertitle = SelectorConfig.get("naver.title");
    public String naverscore = SelectorConfig.get("naver.score");
    public String navertags = SelectorConfig.get("naver.tags");
    public String naverauthor = SelectorConfig.get("naver.author");
    public String naverdescription = SelectorConfig.get("naver.description");
    public String naverplatform = "https://series.naver.com/";

    public String kakaotitle = SelectorConfig.get("kakao.title");
    public String kakaoscore = SelectorConfig.get("kakao.score");
    public String kakaotags = SelectorConfig.get("kakao.tags");
    public String kakaodescription1 = SelectorConfig.get("kakao.description1");
    public String kakaodescription2 = SelectorConfig.get("kakao.description2");
    public String kakaoauthor = SelectorConfig.get("kakao.author");
    public String kakaologinwall = SelectorConfig.get("kakao.loginwall");
    public String kakaoplatform = "https://page.kakao.com";


    public String piatitle = SelectorConfig.get("pia.title");
    public String piascore = SelectorConfig.get("pia.score");
    public String piatags = SelectorConfig.get("pia.tags");
    public String piaauthor = SelectorConfig.get("pia.author");
    public String piadescription = SelectorConfig.get("pia.description");

    public String piaplatform = "https://novelpia.com/";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    // 헤드리스 + 이미지 로딩 차단 (페이지 로드 속도용)
    public static ChromeOptions buildHeadlessOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920x1080");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        options.setExperimentalOption("prefs", prefs);
        return options;
    }

    private String bookUrl;
    private String platform;
    private List<Book> books;
    private SearchQuery input;
    private ThreadLocal<WebDriver> driverPool; // kakao에서만 사용: 이번 검색 세션의 스레드풀 워커당 드라이버 재사용

    public BookCrawler(String bookUrl, String platform, List<Book> books, SearchQuery input) {
        this(bookUrl, platform, books, input, null);
    }

    public BookCrawler(String bookUrl, String platform, List<Book> books, SearchQuery input, ThreadLocal<WebDriver> driverPool) {
        this.bookUrl = bookUrl;
        this.platform = platform;
        this.books = books;
        this.input = input;
        this.driverPool = driverPool;
    }

    @Override
    public void run() {
        if (platform.equals("kakao")) {
            runKakao();
        } else {
            runJsoup();
        }
    }

    // 네이버, 피아: 정적 HTML이라 브라우저 없이 jsoup으로 처리 (가벼움)
    private void runJsoup() {
        String booklink = platform.equals("naver") ? naverplatform + bookUrl : piaplatform + bookUrl;
        Exception lastError = null;

        // 실패시 한번 더 재시도 (네트워크 순간 오류/타임아웃 대비)
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                attemptJsoup(booklink);
                return;
            } catch (Exception e) {
                lastError = e;
            }
        }

        log.error("크롤링 실패: {} ({})", booklink, lastError != null ? lastError.getMessage() : "알 수 없음");
    }

    private void attemptJsoup(String booklink) throws IOException {
        String title;
        int score;
        List<String> booktags = new ArrayList<>();
        String author;
        String description;

        if (platform.equals("naver")) {
            Document doc = Jsoup.connect(booklink).userAgent(USER_AGENT).timeout(10000).get();
            Element titleEl = doc.selectFirst(navertitle);
            Element scoreEl = doc.selectFirst(naverscore);
            Elements tagEls = doc.select(navertags);
            Element authorEl = doc.selectFirst(naverauthor);
            Element descEl = doc.selectFirst(naverdescription);

            title = titleEl.text();
            score = (int) Math.round(Double.parseDouble(scoreEl.text()) * 10);
            for (Element tagEl : tagEls) {
                booktags.add(tagEl.text().replace("#", ""));
            }
            author = authorEl.text();
            description = descEl.text();
        } else { // pia
            Document doc = Jsoup.connect(booklink).userAgent(USER_AGENT).timeout(10000).get();
            Element titleEl = doc.selectFirst(piatitle);
            Element scoreEl = doc.selectFirst(piascore);
            Elements tagEls = doc.select(piatags);
            Element authorEl = doc.selectFirst(piaauthor);
            Element descEl = doc.selectFirst(piadescription);

            title = titleEl.text();
            String tmp = scoreEl.text().replace(",", "");
            score = Integer.parseInt(tmp);
            for (Element tagEl : tagEls) {
                booktags.add(tagEl.text().replace("#", ""));
            }
            author = authorEl.text();
            description = descEl.text();
        }

        if (isFilteredOut(booktags)) {
            return;
        }

        synchronized (books) {
            Book book = new Book(title, score, booktags, author, booklink, description.replaceAll("\\s+", " ").trim(), platform);
            books.add(book);
        }
    }

    // 카카오: Vue/React SPA라 브라우저(JS 실행) 필요
    private void runKakao() {
        String booklink = kakaoplatform + bookUrl + "?tab_type=about";
        Exception lastError = null;

        // 실패시 한번 더 재시도 (타임아웃 짧게 잡은 대신)
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                attemptKakao(booklink);
                return;
            } catch (Exception e) {
                lastError = e;
            }
        }

        log.error("크롤링 실패: {} ({})", booklink, lastError != null ? lastError.getMessage() : "알 수 없음");
    }

    private void attemptKakao(String booklink) {
        // 스레드풀 워커가 이미 만들어둔 드라이버 재사용 (책마다 새로 안 띄움)
        WebDriver driver = driverPool.get();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

        driver.get(booklink);

        // 성인/청불 콘텐츠는 로그인 안 하면 상세정보가 아예 안 뜸 -
        // 8초 타임아웃 다 기다리지 않고 짧게 확인 후 바로 건너뜀
        WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
        try {
            quickWait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaotitle)),
                    ExpectedConditions.presenceOfElementLocated(By.xpath(kakaologinwall))
            ));
        } catch (Exception ignored) {
            // 둘 다 안 뜸 - 진짜 느린 페이지일 수 있으니 아래에서 원래 대기시간(8초)으로 다시 시도
        }
        if (!driver.findElements(By.xpath(kakaologinwall)).isEmpty()) {
            log.info("로그인 필요한 콘텐츠라 건너뜀: {}", booklink);
            return;
        }

        WebElement bookdescription1 = null;
        WebElement bookdescription2 = null;
        WebElement booktitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaotitle)));

        // 평점이 아예 없는 책도 있음 (신작 등) - 없다고 책 전체를 스킵하지 않고 0점 처리
        int score = 0;
        try {
            WebElement bookscore = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaoscore)));
            score = (int) Math.round(Double.parseDouble(bookscore.getText()) * 10);
        } catch (Exception e) {
            log.debug("평점 없음: {}", booklink);
        }

        List<WebElement> taglinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(kakaotags)));
        WebElement bookauthor = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaoauthor)));
        try {
            bookdescription1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaodescription1)));
        } catch (Exception e) {
            if (bookdescription1 == null) {
                bookdescription2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaodescription2)));
            }
        }

        // 설명 중 로드된 요소
        WebElement bookdescription = (bookdescription1 != null) ? bookdescription1 : bookdescription2;

        List<String> booktags = new ArrayList<>();
        for (WebElement link : taglinks) {
            String tagText = link.getText();
            booktags.add(tagText.replace("#", ""));
        }

        if (isFilteredOut(booktags)) {
            return;
        }

        synchronized (books) {
            Book book = new Book(booktitle.getText(), score, booktags, bookauthor.getText(), booklink, bookdescription.getText().replaceAll("\\s+", " ").trim(), platform);
            books.add(book);
        }
    }

    // 태그 연산 필터 (합집합=OR: 하나라도 겹치면 통과 / 교집합=AND: 선택한 태그 전부 있어야 통과)
    boolean isFilteredOut(List<String> booktags) {
        if (input.getTagOperation().equals("합집합")) {
            return input.getTags() != null && !input.getTags().isEmpty() && input.getTags().stream().noneMatch(booktags::contains);
        } else if (input.getTagOperation().equals("교집합")) {
            return input.getTags() != null && input.getTags().stream().anyMatch(tag -> !booktags.contains(tag));
        }
        return false;
    }
}



public class Execution {
    private static final Logger log = LoggerFactory.getLogger(Execution.class);
    private String Series = "https://series.naver.com/novel/categoryProductList.series?categoryTypeCode=all&genreCode=&orderTypeCode=new&is&isFinished=true";
    private String SeriesSearch1 = "https://series.naver.com/search/search.series?t=novel&q=";
    private String SeriesSearch2 = "#";
    private String SeriestagSearch = "https://series.naver.com/novel/categoryProductList.series?categoryTypeCode=genre&genreCode=";
    private String Serieslist = SelectorConfig.get("naver.list");
    private String Seriespage = SelectorConfig.get("naver.page");
    private String SerieslistGenre = SelectorConfig.get("naver.list.genre");
    private String SeriespageGenre = SelectorConfig.get("naver.page.genre");

    private String Kakao = "https://page.kakao.com/menu/10011/screen/84?is_complete=false";
    private String KakaoSearch1 = "https://page.kakao.com/search/result?keyword=";
    private String KakaoSearch2 = "&categoryUid=11";
    private String KakaotagSearch = "https://page.kakao.com/search/themekeyword?filterList=";
    private String Kakaolist = SelectorConfig.get("kakao.list");
    private String Kakaoname = SelectorConfig.get("kakao.name");

    private String Pia = "https://novelpia.com/top100/complete/weekly/view/all/plus";
    private String PiaSearch1 = "https://novelpia.com/search/all//1/";
    private String PiaSearch2 = "?page=1&rows=30&novel_type=&start_count_book=&end_count_book=&novel_age=&start_days=&sort_col=last_viewdate&novel_genre=&block_out=0&block_stop=0&is_contest=0&list_display=list";
    private String Pialist = SelectorConfig.get("pia.list");
    private String Pianame = SelectorConfig.get("pia.name");
    private String Piapage = SelectorConfig.get("pia.page");

    public List<Book> Start(String platform, SearchQuery input, Consumer<String> onProgress) {
        if (platform.equals("naver")) {
            return StartNaver(input, onProgress);
        }

        List<Book> books = Collections.synchronizedList(new ArrayList<>());
        String site = "";
        String search1 = "";
        String search2 = "";
        String tagsearch = "";
        String booklist = "";
        String bookname = "";
        String sitepage = "";

        switch (platform) {
            case "kakao":
                site = Kakao;
                search1 = KakaoSearch1;
                search2 = KakaoSearch2;
                tagsearch = KakaotagSearch;
                booklist = Kakaolist;
                bookname = Kakaoname;
                break;
            case "pia":
                site = Pia;
                search1 = PiaSearch1;
                search2 = PiaSearch2;
                booklist = Pialist;
                bookname = Pianame;
                sitepage = Piapage;
                break;
        }

        // 브라우저 안띄우기 (이미지 로딩도 꺼서 로드 속도 개선)
        WebDriver driver = new ChromeDriver(BookCrawler.buildHeadlessOptions());

        // 검색할 URL 목록 (태그 여러개 선택시 태그별로 각각 검색, 결과는 뒤에서 합침)
        List<String> searchUrls = new ArrayList<>();
        try {
            if (!input.getAuthor().isEmpty()) { // 저자검색
                String encodedInput = URLEncoder.encode(input.getAuthor(), StandardCharsets.UTF_8.toString());
                searchUrls.add(search1 + encodedInput + search2);
            } else if (!input.getTitle().isEmpty()) { // 제목 검색
                String encodedInput = URLEncoder.encode(input.getTitle(), StandardCharsets.UTF_8.toString());
                searchUrls.add(search1 + encodedInput + search2);
            } else { // 태그(장르) 검색: 선택된 태그마다 각각 검색
                for (String tag : input.getTags()) {
                    if (platform.equals("pia")) {
                        String encodedInput = URLEncoder.encode(tag, StandardCharsets.UTF_8.toString());
                        searchUrls.add(search1 + encodedInput + search2);
                    } else { // kakao: 장르는 키워드검색이 아니라 subcategory_uid로 필터링
                        String code = kakaoGenreCode(tag);
                        if (code == null) {
                            log.warn("카카오는 '{}' 장르를 지원하지 않아 건너뜀", tag);
                            continue;
                        }
                        searchUrls.add(site + "&subcategory_uid=" + code);
                    }
                }
            }
        } catch (Exception e) {
            log.error("검색 URL 생성 실패", e);
        }

        onProgress.accept(platform + ": 검색 시작 (" + searchUrls.size() + "개 조건)");

        // 카카오 장르(태그) 검색은 subcategory_uid로 이미 해당 장르만 걸러져서 들어옴.
        // 책 상세페이지의 "키워드" 칩(#육아 등)은 장르명과 무관한 별개 필드라서
        // 거기다 대고 장르명 매칭 필터를 또 돌리면 전부 걸러져버림 -> 이 경우 필터 생략
        boolean kakaoGenreSearch = platform.equals("kakao") && input.getAuthor().isEmpty() && input.getTitle().isEmpty() && !input.getTags().isEmpty();
        SearchQuery crawlerInput = kakaoGenreSearch
                ? new SearchQuery(input.getTitle(), input.getAuthor(), new ArrayList<>(), input.getTagOperation(), input.getPlatforms())
                : input;

        try {
            // 스레드 풀/드라이버풀은 태그 여러개 돌아도 한번만 만들어 재사용
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            Set<WebDriver> sessionDrivers = Collections.synchronizedSet(new HashSet<>());
            ThreadLocal<WebDriver> driverPool = ThreadLocal.withInitial(() -> {
                WebDriver d = new ChromeDriver(BookCrawler.buildHeadlessOptions());
                sessionDrivers.add(d);
                return d;
            });

            for (String url : searchUrls) {
                driver.get(url);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                if (platform.equals("kakao")) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
                    int scrollmax = 10; // 최대 스크롤 시도 횟수
                    int scrollcnt = 0;

                    while (scrollcnt < scrollmax) {
                        // 페이지 끝으로 스크롤
                        js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(booklist)));

                        // 스크롤 높이가 변하지 않으면 종료
                        long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                        if (newHeight == lastHeight) {
                            break;  // 높이가 변하지 않으면 종료
                        }

                        // 마지막 높이를 새로운 높이로 업데이트
                        lastHeight = newHeight;

                        // 시도 횟수 증가
                        scrollcnt++;
                    }

                    // 상품 목록에서 링크 추출
                    List<WebElement> bookLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(booklist)));
                    List<Future<?>> futures = new ArrayList<>();
                    int cnt = 0;

                    // 각 상품에 대해 스레드 풀에 작업을 할당
                    for (WebElement link : bookLinks) {

                        WebElement title = link.findElement(By.cssSelector(bookname));
                        String name = title.getText();
                        if (!input.getTitle().isEmpty() && !name.contains(input.getTitle())) {
                            continue;
                        }

                        String bookUrl = link.getDomAttribute("href");
                        Future<?> future = executorService.submit(new BookCrawler(bookUrl, platform, books, crawlerInput, driverPool));
                        futures.add(future);
                    }

                    // 모든 작업이 완료될 때까지 대기
                    for (Future<?> future : futures) {
                        try {
                            future.get();
                            log.info("{}/{}", ++cnt, bookLinks.size());
                            onProgress.accept(platform + ": " + cnt + "/" + bookLinks.size() + " 처리 중");
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("크롤링 작업 실패", e);
                        }
                    }

                } else {
                    List<WebElement> pageLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sitepage)));
                    log.info("페이지 링크: {}", pageLinks.size());
                    int totalpage = pageLinks.size();
                    int count = 0;

                    for (WebElement pageLink : pageLinks) {
                        String linkText = pageLink.getText();
                        int cnt = 0;
                        count++;
                        log.info("진행률: {}/{} ({})", count, totalpage, pageLink.getText());
                        onProgress.accept(platform + ": 페이지 " + count + "/" + totalpage);

                        // 숫자가 아닌 페이지 버튼은 클릭하지 않도록 필터링
                        if (!linkText.matches("\\d+")) {
                            continue;
                        }

                        // 각 페이지 링크 (naver는 StartNaver로 분리되어 이 경로는 pia만 탐)
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].click();", pageLink);

                        // 상품 목록에서 링크 추출
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(booklist)));
                        List<WebElement> bookLinks = driver.findElements(By.cssSelector(booklist));

                        List<Future<?>> futures = new ArrayList<>();

                        // 각 상품에 대해 스레드 풀에 작업을 할당
                        for (WebElement link : bookLinks) {

                            WebElement title = link.findElement(By.cssSelector(bookname));
                            String name = title.getText();
                            if (!input.getTitle().isEmpty() && !name.contains(input.getTitle())) {
                                continue;
                            }

                            String bookUrl = link.getDomAttribute("href");
                            Future<?> future = executorService.submit(new BookCrawler(bookUrl, platform, books, input));
                            futures.add(future);
                        }

                        // 모든 작업이 완료될 때까지 대기
                        for (Future<?> future : futures) {
                            try {
                                future.get();
                                log.info("{}/{}", ++cnt, bookLinks.size());
                                onProgress.accept(platform + ": " + cnt + "/" + bookLinks.size() + " 처리 중 (페이지 " + count + "/" + totalpage + ")");
                            } catch (InterruptedException | ExecutionException e) {
                                log.error("크롤링 작업 실패", e);
                            }
                        }
                    }
                }
            }

            executorService.shutdown();
            for (WebDriver d : sessionDrivers) {
                try {
                    d.quit();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.error("크롤링 중 오류", e);
            onProgress.accept(platform + ": 오류 발생 - " + e.getMessage());
        } finally {
            driver.quit();
        }
        List<Book> result = dedupeByLink(books);
        onProgress.accept(platform + ": 완료 (" + result.size() + "건)");
        return result;
    }

    // 태그 여러개 검색시 같은 책이 여러 태그 목록에 겹쳐 나올 수 있어 링크 기준으로 중복 제거
    static List<Book> dedupeByLink(List<Book> books) {
        List<Book> result = new ArrayList<>();
        Set<String> seenLinks = new HashSet<>();
        for (Book book : books) {
            if (seenLinks.add(book.getLink())) {
                result.add(book);
            }
        }
        return result;
    }

    // 네이버 장르명 -> 카테고리 코드 (네이버 시리즈에 실제 존재하는 장르 전체)
    static String naverGenreCode(String tag) {
        switch (tag) {
            case "로맨스": return "201";
            case "로판":
            case "로맨스판타지": return "207";
            case "판타지": return "202";
            case "현판":
            case "현대판타지": return "208";
            case "무협": return "206";
            case "미스터리": return "203";
            case "라이트노벨": return "205";
            case "BL": return "209";
            default: return null; // 매핑 안 되는 태그(자유 입력 태그 등)는 네이버에서 검색 불가
        }
    }

    // 카카오페이지 장르명 -> subcategory_uid (실제 사이트에서 확인함, 2026-09 기준)
    static String kakaoGenreCode(String tag) {
        switch (tag) {
            case "판타지": return "86";
            case "현판":
            case "현대판타지": return "120";
            case "로맨스": return "89";
            case "로판":
            case "로맨스판타지": return "117";
            case "무협": return "87";
            case "BL": return "123";
            default: return null; // 매핑 안 되는 태그(자유 입력 태그 등)는 카카오에서 장르 검색 불가
        }
    }

    // 네이버: 정적 HTML이라 브라우저 없이 jsoup으로 목록+페이지네이션 처리 (가벼움)
    private List<Book> StartNaver(SearchQuery input, Consumer<String> onProgress) {
        List<Book> books = Collections.synchronizedList(new ArrayList<>());

        // 검색할 URL 목록 (태그 여러개 선택시 태그별로 각각 검색, 결과는 뒤에서 합침)
        List<String> searchUrls = new ArrayList<>();
        try {
            if (!input.getAuthor().isEmpty()) {
                String encodedInput = URLEncoder.encode(input.getAuthor(), StandardCharsets.UTF_8.toString());
                searchUrls.add(SeriesSearch1 + encodedInput + SeriesSearch2);
            } else if (!input.getTitle().isEmpty()) {
                String encodedInput = URLEncoder.encode(input.getTitle(), StandardCharsets.UTF_8.toString());
                searchUrls.add(SeriesSearch1 + encodedInput + SeriesSearch2);
            } else {
                for (String tag : input.getTags()) {
                    String code = naverGenreCode(tag);
                    if (code == null) {
                        log.warn("네이버는 '{}' 장르를 지원하지 않아 건너뜀", tag);
                        continue;
                    }
                    searchUrls.add(SeriestagSearch + code);
                }
            }
        } catch (Exception e) {
            log.error("검색 URL 생성 실패", e);
        }

        onProgress.accept("naver: 검색 시작 (" + searchUrls.size() + "개 조건)");
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        for (String url : searchUrls) {
            try {
                Document firstPage = Jsoup.connect(url).userAgent(BookCrawler.USER_AGENT).timeout(10000).get();

                // 페이지네이션 링크(숫자만) 수집, 없으면 첫 페이지만
                // 검색결과 페이지(com_srch)와 장르 카테고리 페이지(lst_thum_wrap)는 템플릿이 달라서 셀렉터도 다름
                Set<String> pageUrls = new LinkedHashSet<>();
                Elements pageLinks = firstPage.select(Seriespage);
                if (pageLinks.isEmpty()) {
                    pageLinks = firstPage.select(SeriespageGenre);
                }
                for (Element pageLink : pageLinks) {
                    if (pageLink.text().trim().matches("\\d+")) {
                        pageUrls.add("https://series.naver.com" + pageLink.attr("href"));
                    }
                }
                if (pageUrls.isEmpty()) {
                    pageUrls.add(url);
                }

                int totalPage = pageUrls.size();
                int count = 0;

                for (String pageUrl : pageUrls) {
                    count++;
                    log.info("진행률: {}/{}", count, totalPage);
                    onProgress.accept("naver: 페이지 " + count + "/" + totalPage);

                    Document pageDoc = pageUrl.equals(url) ? firstPage : Jsoup.connect(pageUrl).userAgent(BookCrawler.USER_AGENT).timeout(10000).get();
                    Elements bookLinks = pageDoc.select(Serieslist);
                    if (bookLinks.isEmpty()) {
                        bookLinks = pageDoc.select(SerieslistGenre);
                    }

                    List<Future<?>> futures = new ArrayList<>();
                    for (Element link : bookLinks) {
                        String name = link.text();
                        if (!input.getTitle().isEmpty() && !name.contains(input.getTitle())) {
                            continue;
                        }
                        String bookUrl = link.attr("href");
                        futures.add(executorService.submit(new BookCrawler(bookUrl, "naver", books, input)));
                    }

                    int cnt = 0;
                    for (Future<?> future : futures) {
                        try {
                            future.get();
                            log.info("{}/{}", ++cnt, bookLinks.size());
                            onProgress.accept("naver: " + cnt + "/" + bookLinks.size() + " 처리 중 (페이지 " + count + "/" + totalPage + ")");
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("크롤링 작업 실패", e);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("페이지 조회 실패: {}", url, e);
                onProgress.accept("naver: 오류 발생 - " + e.getMessage());
            }
        }

        executorService.shutdown();
        List<Book> result = dedupeByLink(books);
        onProgress.accept("naver: 완료 (" + result.size() + "건)");
        return result;
    }
}