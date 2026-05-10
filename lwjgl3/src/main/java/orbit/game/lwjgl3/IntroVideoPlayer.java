package orbit.game.lwjgl3;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class IntroVideoPlayer {
    private static final String INTRO_DIR = "assets/introvideo";
    private static final String INTRO_RESOURCE = "introvideo/intro.mp4";
    private static final long MAX_WAIT_SECONDS = 60L;
    private static final int WINDOW_WIDTH = 640;
    private static final int WINDOW_HEIGHT = 480;
    private static final AtomicBoolean played = new AtomicBoolean(false);

    private IntroVideoPlayer() {
    }

    static void playBeforeGameLaunch() {
        if (!played.compareAndSet(false, true)) {
            return;
        }

        File introFile = findIntroVideo();
        if (introFile == null) {
            System.err.println("[IntroVideo] Intro video not found");
            return;
        }

        System.out.println("[IntroVideo] Playing intro: " + introFile.getAbsolutePath());
        if (!playWithJavaFx(introFile)) {
            System.err.println("[IntroVideo] JavaFX playback failed, opening intro with the system video player");
            openWithSystemPlayer(introFile);
        }
    }

    private static boolean playWithJavaFx(File introFile) {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);

        try {
            Platform.startup(() -> playOnFxThread(introFile, finished, started, failed));
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> playOnFxThread(introFile, finished, started, failed));
        } catch (Throwable throwable) {
            System.err.println("[IntroVideo] JavaFX is not available: " + throwable.getMessage());
            return false;
        }

        try {
            finished.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return false;
        }
        return started.get() && !failed.get();
    }

    private static void playOnFxThread(File introFile, CountDownLatch finished, AtomicBoolean started, AtomicBoolean failed) {
        MediaPlayer[] playerRef = new MediaPlayer[1];
        Stage stage = new Stage(StageStyle.DECORATED);

        Runnable finish = () -> {
            MediaPlayer player = playerRef[0];
            if (player != null) {
                player.stop();
                player.dispose();
            }
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

            player.setOnReady(() -> {
                started.set(true);
                stage.show();
                stage.toFront();
                stage.requestFocus();
                player.play();
            });
            player.setOnEndOfMedia(finish);
            player.setOnError(() -> {
                failed.set(true);
                System.err.println("[IntroVideo] Failed to play intro: " + player.getError());
                finish.run();
            });
            media.setOnError(() -> {
                failed.set(true);
                System.err.println("[IntroVideo] Failed to load intro: " + media.getError());
                finish.run();
            });
        } catch (Throwable throwable) {
            failed.set(true);
            System.err.println("[IntroVideo] Intro skipped: " + throwable.getMessage());
            finish.run();
        }
    }

    private static void openWithSystemPlayer(File introFile) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(introFile);
            } else {
                new ProcessBuilder("xdg-open", introFile.getAbsolutePath()).start();
            }
            Thread.sleep(8000L);
        } catch (Throwable throwable) {
            System.err.println("[IntroVideo] System video player fallback failed: " + throwable.getMessage());
        }
    }

    private static File findIntroVideo() {
        File[] dirs = {
            new File(INTRO_DIR),
            new File("introvideo")
        };

        for (File dir : dirs) {
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

        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
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
            System.err.println("[IntroVideo] Failed to extract bundled intro: " + exception.getMessage());
            return null;
        }
    }
}
