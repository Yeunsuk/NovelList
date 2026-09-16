// 클래스 : 제목/점수/태그/작가/링크/설명
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class UI {
    public void First(Consumer<SearchQuery> onSearch) {
        SwingUtilities.invokeLater(() -> {
            // 프레임 생성
            JFrame frame = new JFrame("소설 검색");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(560, 640);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

            // 플랫폼
            JPanel platformSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            platformSection.setBorder(sectionBorder("플랫폼"));
            JCheckBox platform1 = new JCheckBox("Series");
            JCheckBox platform2 = new JCheckBox("KakaoPage");
            JCheckBox platform3 = new JCheckBox("NovelPia");
            platformSection.add(platform1);
            platformSection.add(platform2);
            platformSection.add(platform3);

            // 검색 조건
            JPanel conditionSection = new JPanel(new GridBagLayout());
            conditionSection.setBorder(sectionBorder("검색 조건"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;

            JTextField titleField = new JTextField(18);
            JTextField authorField = new JTextField(18);
            JTextField tagField = new JTextField(18);

            addFormRow(conditionSection, gbc, 0, "제목", titleField);
            addFormRow(conditionSection, gbc, 1, "작가", authorField);
            addFormRow(conditionSection, gbc, 2, "태그 (,로 구분)", tagField);

            // 태그 연산
            JPanel operationSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
            operationSection.setBorder(sectionBorder("태그 연산"));
            JRadioButton operation1 = new JRadioButton("합집합 (∪)");
            JRadioButton operation2 = new JRadioButton("교집합 (∩)");
            operation1.setSelected(true); // 태그 필터가 조용히 무시되는 일 없게 기본값 고정 (합집합)

            ButtonGroup operationGroup = new ButtonGroup();
            operationGroup.add(operation1);
            operationGroup.add(operation2);
            operationSection.add(operation1);
            operationSection.add(operation2);

            // 장르
            JPanel genreSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
            genreSection.setBorder(sectionBorder("장르"));
            JCheckBox genre1 = new JCheckBox("판타지");
            JCheckBox genre2 = new JCheckBox("현대판타지");
            JCheckBox genre3 = new JCheckBox("무협");
            JCheckBox genre4 = new JCheckBox("로맨스");
            genreSection.add(genre1);
            genreSection.add(genre2);
            genreSection.add(genre3);
            genreSection.add(genre4);

            for (JComponent section : new JComponent[]{platformSection, conditionSection, operationSection, genreSection}) {
                section.setAlignmentX(Component.LEFT_ALIGNMENT);
                root.add(section);
                root.add(Box.createVerticalStrut(14));
            }

            frame.add(new JScrollPane(root), BorderLayout.CENTER);

            // 버튼 추가
            JButton submitButton = new JButton("검색");
            submitButton.setFont(submitButton.getFont().deriveFont(Font.BOLD, 15f));
            submitButton.setFocusPainted(false);
            frame.getRootPane().setDefaultButton(submitButton); // FlatLaf가 기본 버튼에 강조색 입혀줌
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

            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 24));
            submitButton.setPreferredSize(new Dimension(0, 42));
            buttonPanel.add(submitButton, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);

            // 프레임 보이기
            frame.setVisible(true);
        });


    }

    private static TitledBorder sectionBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        return border;
    }

    private static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    public void End(List<Book> booklist, String platform) {
        // 프레임 설정
        JFrame frame = new JFrame(platform + " 검색 결과 (" + booklist.size() + "건)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // 결과창 하나 닫는다고 앱 전체가 꺼지면 안 됨
        frame.setSize(460, 640);
        frame.setLocationByPlatform(true);

        // 스크롤 가능한 컨테이너
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setBackground(new Color(0xF2F2F2));

        if (booklist.isEmpty()) {
            JLabel noDataLabel = new JLabel("검색 결과가 없습니다", JLabel.CENTER);
            noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            noDataLabel.setForeground(Color.GRAY);
            noDataLabel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
            panel.add(noDataLabel);
        } else {
            // 각 책을 표시하는 카드 추가
            for (Book book : booklist) {
                panel.add(createBookPanel(book));
                panel.add(Box.createVerticalStrut(10));
            }
        }

        // 스크롤 설정
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        frame.add(scrollPane);
        frame.setVisible(true);
    }

    // Book 정보표시 패널
    private static JPanel createBookPanel(Book book) {
        JPanel bookPanel = new JPanel();
        bookPanel.setLayout(new BoxLayout(bookPanel, BoxLayout.Y_AXIS));
        bookPanel.setBackground(Color.WHITE);
        bookPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        bookPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 각 정보를 라벨 추가 (줄바꿈 안 되는 설명을 JLabel로도 뿌리면 라벨 하나가
        // 엄청 넓어져서 BoxLayout이 다른 라벨들을 가운데 정렬시켜 화면 밖으로 밀어냄 -> 제거)
        JLabel titleLabel = new JLabel(book.getTitle().isEmpty() ? "제목 없음" : book.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));

        JLabel metaLabel = new JLabel(String.format("%s  ·  ★ %d  ·  %s",
                book.getPlatform().isEmpty() ? "없음" : book.getPlatform(),
                book.getScore(),
                book.getAuthor().isEmpty() ? "작가 미정" : book.getAuthor()));
        metaLabel.setFont(metaLabel.getFont().deriveFont(12f));
        metaLabel.setForeground(new Color(0x808080));

        JLabel tagsLabel = new JLabel(book.getTags().isEmpty() ? "태그 없음" : String.join(", ", book.getTags()));
        tagsLabel.setFont(tagsLabel.getFont().deriveFont(12f));
        tagsLabel.setForeground(new Color(0x4A7CC7));

        JLabel linkLabel = new JLabel(book.getLink().isEmpty() ? "링크 없음" : book.getLink());
        linkLabel.setFont(linkLabel.getFont().deriveFont(11f));
        linkLabel.setForeground(new Color(0x1A73E8));

        for (JLabel label : new JLabel[]{titleLabel, metaLabel, tagsLabel}) {
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            bookPanel.add(label);
            bookPanel.add(Box.createVerticalStrut(4));
        }

        // 설명은 줄바꿈되는 텍스트영역으로만 표시
        JTextArea descriptionTextArea = new JTextArea(book.getDescription().isEmpty() ? "설명 미정" : book.getDescription());
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setOpaque(false);
        descriptionTextArea.setFont(descriptionTextArea.getFont().deriveFont(12f));
        descriptionTextArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionTextArea.setSize(360, Short.MAX_VALUE); // 넓이 고정해야 줄바꿈 계산이 제대로 됨
        bookPanel.add(descriptionTextArea);
        bookPanel.add(Box.createVerticalStrut(6));

        linkLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookPanel.add(linkLabel);

        return bookPanel;
    }
}
