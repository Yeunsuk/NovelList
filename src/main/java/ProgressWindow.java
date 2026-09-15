import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

// 검색 진행 상황을 보여주는 작은 창. 로그 파일 말고 화면에서도 진행 상태 확인 가능하게.
public class ProgressWindow {
    private JFrame frame;
    private JLabel statusLabel;
    private JTextArea logArea;

    // 백그라운드 스레드에서 생성되므로 Swing 컴포넌트 구성은 EDT에서 동기적으로 처리
    public ProgressWindow() {
        Runnable build = () -> {
            frame = new JFrame("진행 상황");
            frame.setSize(420, 300);
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            statusLabel = new JLabel("검색 준비 중...", JLabel.CENTER);
            frame.add(statusLabel, BorderLayout.NORTH);

            logArea = new JTextArea();
            logArea.setEditable(false);
            frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

            frame.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            build.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(build);
            } catch (InterruptedException | InvocationTargetException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void update(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            logArea.append(status + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void close() {
        SwingUtilities.invokeLater(frame::dispose);
    }
}
