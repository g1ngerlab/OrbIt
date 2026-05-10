package orbit.game.lwjgl3;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class IntroVideoPlayer {
    private static final String INTRO_DIR = "assets/introvideo";
    private static final String INTRO_RESOURCE = "introvideo/intro.mp4";
    private static final Path LOG_FILE = Paths.get("intro-video.log");
    private static final long MAX_WAIT_SECONDS = 60L;
    private static final int WINDOW_WIDTH = 640;
    private static final int WINDOW_HEIGHT = 480;
    private static final AtomicBoolean played = new AtomicBoolean(false);
    private static volatile int lastWindowX = -1;
    private static volatile int lastWindowY = -1;

    private IntroVideoPlayer() {
    }

    static void playBeforeGameLaunch() {
        if (!played.compareAndSet(false, true)) {
            return;
        }

        File introFile = findIntroVideo();
        if (introFile == null) {
            log("Intro video not found");
            return;
        }
        log("Playing intro: " + introFile.getAbsolutePath());

        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean scheduled = new AtomicBoolean(false);

        try {
            scheduled.set(true);
            Platform.startup(() -> {
                playOnFxThread(introFile, finished);
            });
        } catch (IllegalStateException alreadyStarted) {
            scheduled.set(true);
            Platform.runLater(() -> playOnFxThread(introFile, finished));
        } catch (Throwable throwable) {
            log("JavaFX is not available", throwable);
            return;
        }

        try {
            if (scheduled.get()) {
                finished.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log("Intro interrupted", interruptedException);
        }
    }

    private static void playOnFxThread(File introFile, CountDownLatch finished) {
        MediaPlayer[] playerRef = new MediaPlayer[1];
        Stage stage = new Stage(StageStyle.DECORATED);

        Runnable finish = () -> {
            MediaPlayer player = playerRef[0];
            if (player != null) {
                player.stop();
                player.dispose();
            }
            lastWindowX = (int) Math.round(stage.getX());
            lastWindowY = (int) Math.round(stage.getY());
            stage.close();
            finished.countDown();
        };

        try {
            Media media = new Media(introFile.toURI().toString());
            MediaPlayer player = new MediaPlayer(media);
            playerRef[0] = player;

            MediaView mediaView = new MediaView(player);
            mediaView.setPreserveRatio(true);
            mediaView.fitWidthProperty().bind(stage.widthProperty());
            mediaView.fitHeightProperty().bind(stage.heightProperty());

            StackPane root = new StackPane(mediaView);
            root.setStyle("-fx-background-color: black;");

            Scene scene = new Scene(root, Color.BLACK);
            scene.setOnMouseClicked(event -> finish.run());
            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                    finish.run();
                }
            });

            stage.setScene(scene);
            stage.setTitle("orbit");
            stage.setOnCloseRequest(event -> finish.run());
            stage.setWidth(WINDOW_WIDTH);
            stage.setHeight(WINDOW_HEIGHT);
            stage.setAlwaysOnTop(true);
            stage.centerOnScreen();

            player.setOnEndOfMedia(finish);
            player.setOnError(() -> {
                log("Failed to play intro", player.getError());
                finish.run();
            });
            media.setOnError(() -> {
                log("Failed to load intro", media.getError());
                finish.run();
            });

            stage.show();
            stage.toFront();
            stage.requestFocus();
            player.play();
        } catch (Throwable throwable) {
            log("Intro skipped", throwable);
            finish.run();
        }
    }

    private static File findIntroVideo() {
        File[] dirs = {
            new File(INTRO_DIR),
            new File("introvideo")
        };

        for (File dir : dirs) {
            log("Checking intro dir: " + dir.getAbsolutePath());
            File introFile = findFirstMp4(dir);
            if (introFile != null) {
                return introFile;
            }
        }

        return extractBundledIntroVideo();
    }

    private static File findFirstMp4(File dir) {
        if (!dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles((folder, name) -> name.toLowerCase().endsWith(".mp4"));
        if (files == null || files.length == 0) {
            return null;
        }

        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return files[0];
    }

    private static File extractBundledIntroVideo() {
        ClassLoader classLoader = IntroVideoPlayer.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(INTRO_RESOURCE)) {
            if (inputStream == null) {
                return null;
            }

            Path tempFile = Files.createTempFile("orbit-intro-", ".mp4");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            File file = tempFile.toFile();
            file.deleteOnExit();
            return file;
        } catch (IOException exception) {
            log("Failed to extract bundled intro", exception);
            return null;
        }
    }

    private static void log(String message) {
        String line = "[IntroVideo] " + message;
        System.out.println(line);
        try {
            Files.write(LOG_FILE, (LocalDateTime.now() + " " + line + System.lineSeparator()).getBytes("UTF-8"),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Console logging above is enough if the log file cannot be written.
        }
    }

    private static void log(String message, Throwable throwable) {
        log(message + ": " + throwable);
    }

    static int getLastWindowX() {
        return lastWindowX;
    }

    static int getLastWindowY() {
        return lastWindowY;
    }
}
