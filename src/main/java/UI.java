// 클래스 : 제목/점수/태그/작가/링크/설명
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import java.awt.*;

public class UI {
    public void First(Consumer<SearchQuery> onSearch) {
        SwingUtilities.invokeLater(() -> {
            // 프레임 생성
            JFrame frame = new JFrame("검색 조건 설정");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);

            // 레이아웃 설정
            frame.setLayout(new BorderLayout());

            // 패널 배치
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(0, 2));

            // 플랫폼 체크박스
            panel.add(new JLabel("플랫폼", JLabel.CENTER));
            JPanel platformPanel = new JPanel();
            JCheckBox platform1 = new JCheckBox("Series");
            JCheckBox platform2 = new JCheckBox("KakaoPage");
            JCheckBox platform3 = new JCheckBox("NovelPia");
            platformPanel.add(platform1);
            platformPanel.add(platform2);
            platformPanel.add(platform3);
            panel.add(platformPanel);

            // 연산 라디오 버튼
            panel.add(new JLabel("태그 연산", JLabel.CENTER));
            JPanel operationPanel = new JPanel();
            JRadioButton operation1 = new JRadioButton("∪");
            JRadioButton operation2 = new JRadioButton("∩");
            operation1.setSelected(true); // 태그 필터가 조용히 무시되는 일 없게 기본값 고정 (합집합)

            // 라디오 버튼 그룹
            ButtonGroup operationGroup = new ButtonGroup();
            operationGroup.add(operation1);
            operationGroup.add(operation2);

            operationPanel.add(operation1);
            operationPanel.add(operation2);
            panel.add(operationPanel);

            // 장르 체크박스 
            panel.add(new JLabel("장르", JLabel.CENTER));
            JPanel genrePanel = new JPanel();
            JCheckBox genre1 = new JCheckBox("판타지");
            JCheckBox genre2 = new JCheckBox("현대판타지");
            JCheckBox genre3 = new JCheckBox("무협");
            JCheckBox genre4 = new JCheckBox("로맨스");
            genrePanel.add(genre1);
            genrePanel.add(genre2);
            genrePanel.add(genre3);
            genrePanel.add(genre4);
            panel.add(genrePanel);

            // 태그 텍스트 필드
            panel.add(new JLabel("태그(태그 구분: ,)", JLabel.CENTER));
            JTextField tagField = new JTextField(15);
            panel.add(tagField);

            // 제목 텍스트 필드
            panel.add(new JLabel("제목", JLabel.CENTER));
            JTextField titleField = new JTextField(15);
            panel.add(titleField);

            // 작가 텍스트 필드
            panel.add(new JLabel("작가", JLabel.CENTER));
            JTextField authorField = new JTextField(15);
            panel.add(authorField);

            // 프레임에 패널 추가
            frame.add(panel, BorderLayout.CENTER);

            // 버튼 추가
            JButton submitButton = new JButton("검색");
            submitButton.addActionListener(e -> {
                // UI에서 입력받은 값들을 Book 객체에 저장
                String platforms = "";
                if (platform1.isSelected()) platforms += "Series ";
                if (platform2.isSelected()) platforms += "KakaoPage ";
                if (platform3.isSelected()) platforms += "NovelPia ";

                String operation = "";
                if (operation1.isSelected()) operation += "합집합";
                if (operation2.isSelected()) operation += "교집합";

                List<String> tags = new ArrayList<>();
                if (genre1.isSelected()) tags.add("판타지");
                if (genre2.isSelected()) tags.add("현대판타지");
                if (genre3.isSelected()) tags.add("무협");
                if (genre4.isSelected()) tags.add("로맨스");

                String title = titleField.getText();
                String author = authorField.getText();
                String input_tag = tagField.getText();
                String[] tagarr = input_tag.split(",");

                for (String tag : tagarr) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        tags.add(trimmed);
                    }
                }
                

                // 검색 조건 객체 생성
                SearchQuery query = new SearchQuery(title, author, tags, operation, platforms);

                // 입력한 값을 출력
                JOptionPane.showMessageDialog(frame, "플랫폼: " + platforms + "\n연산: " + operation + "\n태그: " + tags + "\n타이틀: " + title);

                // 검색 중 연타하면 크롬 인스턴스가 중복으로 뜨니 끝날 때까지 버튼 비활성화
                submitButton.setEnabled(false);
                new Thread(() -> {
                    try {
                        onSearch.accept(query);
                    } finally {
                        SwingUtilities.invokeLater(() -> submitButton.setEnabled(true));
                    }
                }).start();
            });
            
            // 프레임 하단에 버튼 배치
            frame.add(submitButton, BorderLayout.SOUTH);

            // 프레임 보이기
            frame.setVisible(true);
        });

        
    }

    public void End(List<Book> booklist, String platform) {
        // 프레임 설정
        JFrame frame = new JFrame(platform);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 결과창 하나 닫는다고 앱 전체가 꺼지면 안 됨
        frame.setSize(400, 600);

        // 스크롤 가능한 컨테이너
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if (booklist.isEmpty()) {
            JLabel noDataLabel = new JLabel("No data", JLabel.CENTER);
            noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(noDataLabel);
        } else {
            // 각 책을 표시하는 레이블 추가
            for (Book book : booklist) {
                panel.add(createBookPanel(book));
                panel.add(Box.createVerticalStrut(10));
            }
        }

        // 스크롤 설정
        JScrollPane scrollPane = new JScrollPane(panel);

        frame.add(scrollPane);
        frame.setVisible(true);
    }

    // Book 정보표시 패널
    private static JPanel createBookPanel(Book book) {
        JPanel bookPanel = new JPanel();
        bookPanel.setLayout(new BoxLayout(bookPanel, BoxLayout.Y_AXIS));
        bookPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        bookPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 각 정보를 라벨 추가 (줄바꿈 안 되는 설명을 JLabel로도 뿌리면 라벨 하나가
        // 엄청 넓어져서 BoxLayout이 다른 라벨들을 가운데 정렬시켜 화면 밖으로 밀어냄 -> 제거)
        JLabel titleLabel = new JLabel("Title: " + (book.getTitle().isEmpty() ? "없음" : book.getTitle()));
        JLabel scoreLabel = new JLabel("Score: " + book.getScore());
        JLabel tagsLabel = new JLabel("Tags: " + (book.getTags().isEmpty() ? "없음" : book.getTags()));
        JLabel authorLabel = new JLabel("Author: " + (book.getAuthor().isEmpty() ? "미정" : book.getAuthor()));
        JLabel linkLabel = new JLabel("Link: " + (book.getLink().isEmpty() ? "없음" : book.getLink()));
        JLabel platformLabel = new JLabel("Platform: " + (book.getPlatform().isEmpty() ? "없음" : book.getPlatform()));

        for (JLabel label : new JLabel[]{titleLabel, scoreLabel, tagsLabel, authorLabel, linkLabel, platformLabel}) {
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            bookPanel.add(label);
        }

        // 설명은 줄바꿈되는 텍스트영역으로만 표시
        JTextArea descriptionTextArea = new JTextArea("Description: " + (book.getDescription().isEmpty() ? "미정" : book.getDescription()));
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionTextArea.setSize(360, Short.MAX_VALUE); // 넓이 고정해야 줄바꿈 계산이 제대로 됨
        bookPanel.add(descriptionTextArea);

        return bookPanel;
    }
}