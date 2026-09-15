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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
//입력예시 : Book{title='없음', score=0, tags=[현대판타지, 무협, fgdf, df], author='미정', link='없음', description='합집합', platform='Series KakaoPage '}
// 카카오 스크롤롤
class BookCrawler implements Runnable {
    public String navertitle = "#content > div.end_head > h2";
    public String naverscore = "#content > div.end_head > div.score_area em";
    public String navertags = "#content li.info_lst > ul > li:nth-child(2) > span > a";
    public String naverauthor = "#content li.info_lst > ul > li:nth-child(3) a";
    public String naverdescription = "div#content div.end_dsc div:first-child";
    public String naverplatform = "https://series.naver.com/";

    public String kakaotitle = "div.mb-28pxr.flex.w-320pxr.flex-col div.flex.flex-col.items-center span.font-large3-bold.mb-3pxr.text-ellipsis.break-all.text-el-70.line-clamp-2";
    public String kakaoscore = "div.mb-28pxr.flex.w-320pxr.flex-col div.flex.flex-col.items-center div.flex.items-center:nth-of-type(3) span";
    public String kakaotags = "div.mb-28pxr.ml-4px.flex.w-632pxr.flex-col.overflow-hidden.rounded-12pxr div.flex.w-full.flex-col.items-center.overflow-hidden div[class*='flex-wrap'] span";
    public String kakaodescription1 = "div.mb-28pxr.ml-4px.flex.w-632pxr.flex-col.overflow-hidden.rounded-12pxr div.flex.w-full.flex-col.items-center.overflow-hidden div[class*='pb-10pxr'][class*='cursor-pointer']";
    public String kakaodescription2 = "div.mb-28pxr.ml-4px.flex.w-632pxr.flex-col.overflow-hidden.rounded-12pxr div.flex.w-full.flex-col.items-center.overflow-hidden div[class*='max-h-[216px]']";
    public String kakaoauthor = "div.mb-28pxr.flex.w-320pxr.flex-col div.flex.flex-col.items-center span.font-small2.mb-6pxr.text-ellipsis.text-el-70.opacity-70.break-word-anywhere.line-clamp-2";
    public String kakaoplatform = "https://page.kakao.com";
    

    public String piatitle = "div.epnew-novel-info > div.ep-info-line.epnew-novel-title";
    public String piascore = "div.epnew-novel-info > div.ep-info-line.info-count1 > div.counter-line-a > p:nth-child(2) > span:nth-child(2)";
    public String piatags = "div.epnew-novel-info > div.mobile_hidden > div.ep-info-line.epnew-tag > p.writer-tag span";
    public String piaauthor = "div.epnew-novel-info > div.ep-info-line.epnew-writer > p.in-writer > a";
    public String piadescription = "div.epnew-novel-info > div.mobile_hidden > div.info-graybox > div.synopsis";
    
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
    private Book input;
    private ThreadLocal<WebDriver> driverPool; // kakao에서만 사용: 이번 검색 세션의 스레드풀 워커당 드라이버 재사용

    public BookCrawler(String bookUrl, String platform, List<Book> books, Book input) {
        this(bookUrl, platform, books, input, null);
    }

    public BookCrawler(String bookUrl, String platform, List<Book> books, Book input, ThreadLocal<WebDriver> driverPool) {
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
        String booklink = "";
        try {
            String title;
            int score;
            List<String> booktags = new ArrayList<>();
            String author;
            String description;

            if (platform.equals("naver")) {
                booklink = naverplatform + bookUrl;
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
                booklink = piaplatform + bookUrl;
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
        } catch (Exception e) {
            System.out.println("오류 발생: " + booklink);
            System.out.println("오류 원인: " + e.getMessage());
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

        System.out.println("오류 발생: " + booklink);
        System.out.println("오류 원인: " + (lastError != null ? lastError.getMessage() : "알 수 없음"));
    }

    private void attemptKakao(String booklink) {
        // 스레드풀 워커가 이미 만들어둔 드라이버 재사용 (책마다 새로 안 띄움)
        WebDriver driver = driverPool.get();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

        driver.get(booklink);

        WebElement bookdescription1 = null;
        WebElement bookdescription2 = null;
        WebElement booktitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaotitle)));
        WebElement bookscore = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaoscore)));
        int score = (int) Math.round(Double.parseDouble(bookscore.getText()) * 10);
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

    // 태그 연산 필터 (합집합/교집합)
    private boolean isFilteredOut(List<String> booktags) {
        if (input.getDescription().equals("합집합")) {
            return input.getTags() != null && input.getTags().stream().anyMatch(tag -> !booktags.contains(tag));
        } else if (input.getDescription().equals("교집합")) {
            return input.getTags() != null && booktags.stream().noneMatch(input.getTags()::contains);
        }
        return false;
    }
}



public class Execution {
    private String Series = "https://series.naver.com/novel/categoryProductList.series?categoryTypeCode=all&genreCode=&orderTypeCode=new&is&isFinished=true";
    private String SeriesSearch1 = "https://series.naver.com/search/search.series?t=novel&q=";
    private String SeriesSearch2 = "#";
    private String SeriestagSearch = "https://series.naver.com/novel/categoryProductList.series?categoryTypeCode=genre&genreCode=";
    private String Serieslist = "#content > div.com_srch > div:nth-child(5) > ul > li > div > h3 > a";
    private String Seriespage = "#content > div.com_srch > p > a";

    private String Kakao = "https://page.kakao.com/menu/10011/screen/84?is_complete=false";
    private String KakaoSearch1 = "https://page.kakao.com/search/result?keyword=";
    private String KakaoSearch2 = "&categoryUid=11";
    private String KakaotagSearch = "https://page.kakao.com/search/themekeyword?filterList=";
    private String Kakaolist = "#__next > div > div.flex.w-full.grow.flex-col.px-122pxr > div.mb-84pxr > div.w-full.overflow-hidden.my-5pxr > div > div > div > a";
    private String Kakaoname = "#__next > div > div.flex.w-full.grow.flex-col.px-122pxr > div.mb-84pxr > div.w-full.overflow-hidden.my-5pxr > div > div > div > a > div > div.flex.flex-col > span";

    private String Pia = "https://novelpia.com/top100/complete/weekly/view/all/plus";
    private String PiaSearch1 = "https://novelpia.com/search/all//1/";
    private String PiaSearch2 = "?page=1&rows=30&novel_type=&start_count_book=&end_count_book=&novel_age=&start_days=&sort_col=last_viewdate&novel_genre=&block_out=0&block_stop=0&is_contest=0&list_display=list";
    private String Pialist = "#search_content > div.rand-lists.list > div.rand-wrapper > div.rand-item-wrapper > div > a";
    private String Pianame = "#search_content > div.rand-lists.list > div.rand-wrapper > div.rand-item-wrapper > div > a > div > div.item-txt";
    private String Piapage = "a.page-link";

    public List<Book> Start(String platform, Book input) {
        if (platform.equals("naver")) {
            return StartNaver(input);
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

        try {
             // 웹 페이지 열기
            if (!input.getAuthor().isEmpty()) { // 저자검색
                String encodedInput = URLEncoder.encode(input.getAuthor(), StandardCharsets.UTF_8.toString());
                driver.get(search1 + encodedInput + search2);
            } else if (!input.getTitle().isEmpty()) { // 제목 검색 
                String encodedInput = URLEncoder.encode(input.getTitle(), StandardCharsets.UTF_8.toString());
                driver.get(search1 + encodedInput + search2);
                //System.out.println(search1 + encodedInput + search2);
            } else if (!input.getTags().isEmpty()) { // 태그(장르) 검색
                String tmpTag = input.getTags().get(0);
                String encodedInput = URLEncoder.encode(tmpTag, StandardCharsets.UTF_8.toString());

                if (platform.equals("pia")) {
                    driver.get(search1 + encodedInput + search2);
                } else { // kakao
                    driver.get(tagsearch + encodedInput);
                }
            }


            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            if (platform.equals("kakao")) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
                int scrollmax = 0;
                int scrollcnt = 10;

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

                // 스레드 풀 생성
                ExecutorService executorService = Executors.newFixedThreadPool(4);
                List<Future<?>> futures = new ArrayList<>();
                int cnt = 0;

                // 이번 검색 세션 전용 드라이버풀 (스레드풀 워커당 하나씩 재사용, 책마다 새로 안 띄움)
                Set<WebDriver> sessionDrivers = Collections.synchronizedSet(new HashSet<>());
                ThreadLocal<WebDriver> driverPool = ThreadLocal.withInitial(() -> {
                    WebDriver d = new ChromeDriver(BookCrawler.buildHeadlessOptions());
                    sessionDrivers.add(d);
                    return d;
                });

                // 각 상품에 대해 스레드 풀에 작업을 할당
                for (WebElement link : bookLinks) {

                    WebElement title = link.findElement(By.cssSelector(bookname));
                    String name = title.getText();
                    if (!input.getTitle().isEmpty() && !name.contains(input.getTitle())) {
                        continue;
                    }

                    String bookUrl = link.getDomAttribute("href");
                    Future<?> future = executorService.submit(new BookCrawler(bookUrl, platform, books, input, driverPool));
                    futures.add(future);
                }

                // 모든 작업이 완료될 때까지 대기
                for (Future<?> future : futures) {
                    try {
                        future.get();
                        System.out.println(++cnt+ "/" + bookLinks.size());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                executorService.shutdown();
                for (WebDriver d : sessionDrivers) {
                    try {
                        d.quit();
                    } catch (Exception ignored) {
                    }
                }

            } else {
                List<WebElement> pageLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sitepage)));
                System.out.println("페이지 링크: "+pageLinks.size());
                int totalpage = pageLinks.size();
                int count = 0;

                // 페이지마다 새로 만들지 않고 하나만 만들어 재사용
                ExecutorService executorService = Executors.newFixedThreadPool(4);

                for (WebElement pageLink : pageLinks) {
                    String linkText = pageLink.getText();
                    int cnt = 0;
                    count++;
                    System.out.println("진행률: "+count+ "/" +totalpage);
                    System.out.println(pageLink.getText());
                                          
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
                            System.out.println(++cnt+ "/" + bookLinks.size());
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
                executorService.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return books;
    }

    // 네이버: 정적 HTML이라 브라우저 없이 jsoup으로 목록+페이지네이션 처리 (가벼움)
    private List<Book> StartNaver(Book input) {
        List<Book> books = Collections.synchronizedList(new ArrayList<>());
        String url = Series;

        try {
            if (!input.getAuthor().isEmpty()) {
                String encodedInput = URLEncoder.encode(input.getAuthor(), StandardCharsets.UTF_8.toString());
                url = SeriesSearch1 + encodedInput + SeriesSearch2;
            } else if (!input.getTitle().isEmpty()) {
                String encodedInput = URLEncoder.encode(input.getTitle(), StandardCharsets.UTF_8.toString());
                url = SeriesSearch1 + encodedInput + SeriesSearch2;
            } else if (!input.getTags().isEmpty()) {
                String tmpTag = input.getTags().get(0);
                String code = "";
                switch (tmpTag) {
                    case "로맨스": code = "201"; break;
                    case "판타지": code = "202"; break;
                    case "무협": code = "206"; break;
                    case "현대판타지": code = "208"; break;
                }
                url = SeriestagSearch + code;
            }

            Document firstPage = Jsoup.connect(url).userAgent(BookCrawler.USER_AGENT).timeout(10000).get();

            // 페이지네이션 링크(숫자만) 수집, 없으면 첫 페이지만
            Set<String> pageUrls = new LinkedHashSet<>();
            Elements pageLinks = firstPage.select(Seriespage);
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
                System.out.println("진행률: " + count + "/" + totalPage);

                Document pageDoc = pageUrl.equals(url) ? firstPage : Jsoup.connect(pageUrl).userAgent(BookCrawler.USER_AGENT).timeout(10000).get();
                Elements bookLinks = pageDoc.select(Serieslist);

                ExecutorService executorService = Executors.newFixedThreadPool(8);
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
                        System.out.println(++cnt + "/" + bookLinks.size());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                executorService.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return books;
    }
}