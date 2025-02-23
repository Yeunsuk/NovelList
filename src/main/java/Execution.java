import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private String bookUrl;
    private String platform;
    private List<Book> books;
    private Book input;

    public BookCrawler(String bookUrl, String platform, List<Book> books, Book input) {
        this.bookUrl = bookUrl;
        this.platform = platform;
        this.books = books;
        this.input = input;
    }

    @Override
    public void run() {
        // 브라우저 안띄우기
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920x1080");
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        String booklink = "";
        

        try {
            // 플랫폼에 맞는 CSS 선택자 설정
            WebElement booktitle = null;
            WebElement bookscore = null;
            List<WebElement> taglinks = null;
            WebElement bookauthor = null;
            WebElement bookdescription = null;
            List<String> booktags = new ArrayList<>();
            int score = 0;

            // 플랫폼에 따라 다른 CSS 선택자 사용
            switch (platform) {
                case "naver":
                    booklink = naverplatform + bookUrl;
                    driver.get(booklink);
                    booktitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(navertitle)));
                    bookscore = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(naverscore)));
                    score = (int) Math.round(Double.parseDouble(bookscore.getText()) * 10);
                    taglinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(navertags)));
                    bookauthor = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(naverauthor)));
                    bookdescription = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(naverdescription)));
                    break;
                case "kakao":
                    booklink = kakaoplatform + bookUrl + "?tab_type=about";
                    driver.get(booklink);

                    WebElement bookdescription1 = null;
                    WebElement bookdescription2 = null;
                    booktitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaotitle)));
                    bookscore = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaoscore)));
                    score = (int) Math.round(Double.parseDouble(bookscore.getText()) * 10);
                    taglinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(kakaotags)));
                    bookauthor = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaoauthor)));
                    try {
                        bookdescription1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaodescription1)));
                    } catch (Exception e) {
                        if (bookdescription1 == null) {
                            bookdescription2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(kakaodescription2)));
                        }
                    }
                    
                    // 설명 중 로드된 요소
                    bookdescription = (bookdescription1 != null) ? bookdescription1 : bookdescription2;
                    break;
                case "pia":
                    booklink = piaplatform + bookUrl;
                    driver.get(booklink);
                    booktitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(piatitle)));
                    bookscore = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(piascore)));
                    String tmp = bookscore.getText().replace(",", "");
                    score = Integer.parseInt(tmp);
                    taglinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(piatags)));
                    bookauthor = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(piaauthor)));
                    bookdescription = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(piadescription)));
                    break;
            }

            for (WebElement link : taglinks) {
                String tagText = link.getText();
                booktags.add(tagText.replace("#", ""));
            }

            /* 
            System.out.println("제목: " + booktitle.getText());
            System.out.println("점수: " + score);
            System.out.println("태그: " + booktags);
            System.out.println("저자: " + bookauthor.getText());
            System.out.println("정보: " + bookdescription.getText());*/

            // 태그 연산 필터
            if (input.getDescription() == "합집합") {
                if (input.getTags() != null && input.getTags().stream().anyMatch(tag -> !booktags.contains(tag))) {
                    return;
                }
            } else if (input.getDescription() == "교집합") {
                if (input.getTags() != null && booktags.stream().noneMatch(input.getTags()::contains)) {
                    return;
                }
            }

            // 책 정보 추가
            //System.out.println(booktags); 
            synchronized (books) {
                Book book = new Book(booktitle.getText(), score, booktags, bookauthor.getText(), booklink, bookdescription.getText().replaceAll("\\s+", " ").trim(), platform);
                books.add(book);
            }

        } catch (Exception e) {
            System.out.println("오류 발생: " + booklink);
            System.out.println("오류 원인: " + e.getMessage());
        } finally {
            driver.quit();
        }
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
        List<Book> books = Collections.synchronizedList(new ArrayList<>());
        String site = "";
        String search1 = "";
        String search2 = "";
        String tagsearch = "";
        String booklist = "";
        String bookname = "";
        String sitepage = "";

        switch (platform) {
            case "naver":
                site = Series;
                search1 = SeriesSearch1;
                search2 = SeriesSearch2;
                tagsearch = SeriestagSearch;
                booklist = Serieslist;
                bookname = Serieslist;
                sitepage = Seriespage;
                break;
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

        // 브라우저 안띄우기
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920x1080");
        WebDriver driver = new ChromeDriver(options);
        

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

                if (platform == "pia") {
                    driver.get(search1 + encodedInput + search2);
                } else if (platform == "kakao") {
                    driver.get(tagsearch + encodedInput);
                } else {
                    String code = "";  
                    switch (tmpTag) {
                        case "로맨스":
                            code = "201";
                            break;
                        case "판타지":
                            code = "202";
                            break;
                        case "무협":
                            code = "206";
                            break;
                        case "현대판타지":
                            code = "208";
                            break;
                    }
                    driver.get(tagsearch + code);
                }
            }


            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            if (platform == "kakao") {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
                int scrollcnt = 0;
                int scrollmax = 10;

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
            
                executorService.shutdown();
            
            } else {
                List<WebElement> pageLinks = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(sitepage)));
                System.out.println("페이지 링크: "+pageLinks.size());
                int totalpage = pageLinks.size();
                int count = 0;
                
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
            
                    // 각 페이지 링크
                    if (platform == "naver") {
                        String page = pageLink.getDomAttribute("href");
                        driver.get("https://series.naver.com/" + page);
                    } else {
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].click();", pageLink);
                    }
            
                    // 상품 목록에서 링크 추출
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(booklist)));
                    List<WebElement> bookLinks = driver.findElements(By.cssSelector(booklist));
            
                    // 스레드 풀 생성
                    ExecutorService executorService = Executors.newFixedThreadPool(4);
                    List<Future<?>> futures = new ArrayList<>();
            
                    // 각 상품에 대해 스레드 풀에 작업을 할당
                    for (WebElement link : bookLinks) {

                        if (platform == "naver") {
                            String name = link.getText();
                            if (!input.getTitle().isEmpty() && !name.contains(input.getTitle())) {
                                continue;
                            }
                        } else {
                            WebElement title = link.findElement(By.cssSelector(bookname));
                            String name = title.getText();
                            if (!input.getTitle().isEmpty() && !name.contains(input.getTitle())) {
                                continue;
                            }
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
            
                    executorService.shutdown();
                }
            }   
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return books;
    }
}