package fastvideostream;

import fastscreen.FastScreen;
import fasttheme.FastTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Swing control window for the headless FastVideoStream CLI. */
public final class FastVideoStreamApp extends JFrame {
    private final JTextField monitorField = new JTextField("0", 5);
    private final JTextField fpsField = new JTextField("60", 5);
    private final JTextField bitrateField = new JTextField("6000", 6);
    private final JTextField encoderField = new JTextField("h264_nvenc", 12);
    private final JTextField ffmpegField = new JTextField("ffmpeg", 16);
    private final JTextField cameraXField = new JTextField("20", 5);
    private final JTextField cameraYField = new JTextField("20", 5);
    private final JTextField cameraWField = new JTextField("480", 5);
    private final JTextField cameraHField = new JTextField("270", 5);
    private final JCheckBox cameraCheck = new JCheckBox("Camera PiP");
    private final JCheckBox cursorCheck = new JCheckBox("Cursor", true);
    private final JCheckBox microphoneCheck = new JCheckBox("Microphone");
    private final JCheckBox systemAudioCheck = new JCheckBox("System audio");
    private final JCheckBox youtubeCheck = new JCheckBox("YouTube", true);
    private final JCheckBox twitchCheck = new JCheckBox("Twitch", true);
    private final JPasswordField youtubeKeyField = new JPasswordField(16);
    private final JPasswordField twitchKeyField = new JPasswordField(16);
    private final JLabel statusLabel = new JLabel("Ready");
    private Process streamProcess;

    private FastVideoStreamApp() {
        setTitle("FastVideoStream 0.1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 560);
        setMinimumSize(new Dimension(560, 500));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        add(createForm(), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 5, 4, 5);
        constraints.weightx = 1.0;
        int row = 0;

        addRow(form, constraints, row++, "Monitor:", monitorField);
        addRow(form, constraints, row++, "FPS:", fpsField);
        addRow(form, constraints, row++, "Bitrate (kbit/s):", bitrateField);
        addRow(form, constraints, row++, "Encoder:", encoderField);
        addRow(form, constraints, row++, "FFmpeg:", ffmpegField);

        JPanel cameraPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cameraPanel.add(cameraCheck);
        cameraPanel.add(new JLabel("x"));
        cameraPanel.add(cameraXField);
        cameraPanel.add(new JLabel("y"));
        cameraPanel.add(cameraYField);
        cameraPanel.add(new JLabel("w"));
        cameraPanel.add(cameraWField);
        cameraPanel.add(new JLabel("h"));
        cameraPanel.add(cameraHField);
        addRow(form, constraints, row++, "Camera:", cameraPanel);

        JPanel audioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        audioPanel.add(microphoneCheck);
        audioPanel.add(systemAudioCheck);
        addRow(form, constraints, row++, "Audio:", audioPanel);

        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        outputPanel.add(youtubeCheck);
        outputPanel.add(new JLabel("Key"));
        outputPanel.add(youtubeKeyField);
        outputPanel.add(twitchCheck);
        outputPanel.add(new JLabel("Key"));
        outputPanel.add(twitchKeyField);
        addRow(form, constraints, row++, "Targets:", outputPanel);

        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        flagsPanel.add(cursorCheck);
        addRow(form, constraints, row, "Capture:", flagsPanel);
        return form;
    }

    private static void addRow(JPanel panel, GridBagConstraints template, int row, String label, java.awt.Component component) {
        GridBagConstraints left = (GridBagConstraints) template.clone();
        left.gridx = 0;
        left.gridy = row;
        left.weightx = 0;
        panel.add(new JLabel(label), left);

        GridBagConstraints right = (GridBagConstraints) template.clone();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        panel.add(component, right);
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new BorderLayout(8, 8));
        actions.setBorder(BorderFactory.createEmptyBorder(8, 18, 16, 18));
        JButton startButton = new JButton("Start Streaming");
        JButton stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        startButton.addActionListener(event -> {
            try {
                startStream();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            } catch (Exception exception) {
                statusLabel.setText("Error: " + exception.getMessage());
            }
        });
        stopButton.addActionListener(event -> {
            stopStream();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(startButton);
        buttons.add(stopButton);
        actions.add(buttons, BorderLayout.WEST);
        actions.add(statusLabel, BorderLayout.CENTER);
        return actions;
    }

    private void startStream() throws Exception {
        String youtubeKey = new String(youtubeKeyField.getPassword()).trim();
        String twitchKey = new String(twitchKeyField.getPassword()).trim();
        if (!youtubeCheck.isSelected() && !twitchCheck.isSelected()) {
            throw new IllegalArgumentException("Select YouTube or Twitch.");
        }
        if (youtubeCheck.isSelected() && youtubeKey.isEmpty()) {
            throw new IllegalArgumentException("YouTube key is required.");
        }
        if (twitchCheck.isSelected() && twitchKey.isEmpty()) {
            throw new IllegalArgumentException("Twitch key is required.");
        }

        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("fastvideostream.FastVideoStreamCli");
        command.add("--monitor=" + monitorField.getText().trim());
        command.add("--fps=" + fpsField.getText().trim());
        command.add("--bitrate=" + bitrateField.getText().trim());
        command.add("--encoder=" + encoderField.getText().trim());
        command.add("--ffmpeg=" + ffmpegField.getText().trim());
        if (cameraCheck.isSelected()) {
            command.add("--camera=" + cameraXField.getText().trim() + "," + cameraYField.getText().trim()
                    + "," + cameraWField.getText().trim() + "," + cameraHField.getText().trim());
        }
        if (cursorCheck.isSelected()) command.add("--cursor");
        if (microphoneCheck.isSelected()) command.add("--microphone");
        if (systemAudioCheck.isSelected()) command.add("--system-audio");

        ProcessBuilder builder = new ProcessBuilder(command).inheritIO();
        if (youtubeCheck.isSelected()) builder.environment().put("FAST_YOUTUBE_KEY", youtubeKey);
        if (twitchCheck.isSelected()) builder.environment().put("FAST_TWITCH_KEY", twitchKey);
        streamProcess = builder.start();
        statusLabel.setText("Streaming...");
    }

    private void stopStream() {
        if (streamProcess != null && streamProcess.isAlive()) streamProcess.destroy();
        streamProcess = null;
        statusLabel.setText("Stopped");
    }

    private static String javaExecutable() {
        File java = new File(System.getProperty("java.home"), "bin/java.exe");
        return java.exists() ? java.getAbsolutePath() : "java";
    }

    private void excludeWindow() {
        try {
            long hwnd = FastTheme.getWindowHandle(this);
            if (hwnd != 0 && FastScreen.setWindowExcluded(hwnd, true)) {
                statusLabel.setText("Ready (window excluded from capture)");
            }
        } catch (Throwable exception) {
            statusLabel.setText("Ready (window exclusion unavailable)");
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            FastVideoStreamApp window = new FastVideoStreamApp();
            window.setVisible(true);
            window.excludeWindow();
        });
    }
}
