import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// 크롤링 CSS 셀렉터 설정 로더. 사이트 구조 바뀌었을 때 재빌드 없이
// 실행파일 옆 selectors.properties만 고치면 반영되게 하기 위함.
public class SelectorConfig {
    private static final Properties props = new Properties();

    static {
        // 1. 기본값: jar 안에 번들된 설정
        try (InputStream in = SelectorConfig.class.getResourceAsStream("/selectors.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.out.println("기본 셀렉터 설정 로드 실패: " + e.getMessage());
        }

        // 2. 실행파일 옆에 같은 이름의 파일 있으면 덮어쓰기 (재빌드 없이 패치 가능)
        File external = new File("selectors.properties");
        if (external.exists()) {
            try (InputStream in = new FileInputStream(external)) {
                props.load(in);
                System.out.println("외부 셀렉터 설정 적용됨: " + external.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("외부 셀렉터 설정 로드 실패: " + e.getMessage());
            }
        }
    }

    public static String get(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("셀렉터 설정 없음: " + key);
        }
        return value;
    }
}
