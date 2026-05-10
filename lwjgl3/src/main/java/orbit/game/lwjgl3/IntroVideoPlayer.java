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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class IntroVideoPlayer {
    private static final String INTRO_DIR = "assets/introvideo";
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
            System.err.println("[IntroVideo] Intro video not found");
            return;
        }
        System.out.println("[IntroVideo] Playing intro: " + introFile.getAbsolutePath());

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
            System.err.println("[IntroVideo] JavaFX is not available: " + throwable.getMessage());
            return;
        }

        try {
            if (scheduled.get()) {
                finished.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
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
                System.err.println("[IntroVideo] Failed to play intro: " + player.getError());
                finish.run();
            });
            media.setOnError(() -> {
                System.err.println("[IntroVideo] Failed to load intro: " + media.getError());
                finish.run();
            });

            stage.show();
            stage.toFront();
            stage.requestFocus();
            player.play();
        } catch (Throwable throwable) {
            System.err.println("[IntroVideo] Intro skipped: " + throwable.getMessage());
            finish.run();
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

        return null;
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

    static int getLastWindowX() {
        return lastWindowX;
    }

    static int getLastWindowY() {
        return lastWindowY;
    }
}
