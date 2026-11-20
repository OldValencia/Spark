package to.sparkapp.app.ui.topbar.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import to.sparkapp.app.support.JavaFxTestRuntime;

@Tag("native")
@Timeout(20)
class DockSmoothScrollTest {
    private static final double VISIBLE = 200;
    private static final double CONTENT = 1000;
    private static final double OVERFLOW = CONTENT - VISIBLE;

    private final AtomicBoolean reducedMotion = new AtomicBoolean();
    private Stage stage;
    private ScrollPane pane;
    private DockSmoothScroll scroll;

    @BeforeEach
    void showTabs() throws Exception {
        JavaFxTestRuntime.call(() -> {
            var tabs = new Region();
            tabs.setMinWidth(CONTENT);
            tabs.setPrefSize(CONTENT, 30);
            var content = new HBox(tabs);
            pane = new ScrollPane(content);
            pane.setStyle("-fx-padding: 0; -fx-background-insets: 0;");
            pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            pane.setPrefSize(VISIBLE, 30);
            pane.setMaxWidth(VISIBLE);
            stage = new Stage();
            stage.setX(-20000);
            stage.setY(-20000);
            stage.setScene(new Scene(new HBox(pane), VISIBLE, 30));
            stage.show();
            scroll = new DockSmoothScroll(pane, content, reducedMotion::get);

            return null;
        });
    }

    @AfterEach
    void closeStage() throws Exception {
        JavaFxTestRuntime.call(() -> {
            scroll.stop();
            stage.close();

            return null;
        });
    }

    @Test
    void aStepGlidesInsteadOfJumping() throws Exception {
        onFx(() -> scroll.scrollBy(100));
        var firstFrame = offset();

        assertTrue(firstFrame < 100, "The tabs do not jump straight to the end: " + firstFrame);

        Thread.sleep(600);

        assertEquals(100, offset(), 1);
    }

    @Test
    void quickStepsAddUpAndStopAtTheEnd() throws Exception {
        onFx(() -> {
            scroll.scrollBy(300);
            scroll.scrollBy(300);
        });
        Thread.sleep(600);

        assertEquals(600, offset(), 1, "The second step continues from where the first was heading");

        onFx(() -> scroll.scrollBy(500));
        Thread.sleep(600);

        assertEquals(OVERFLOW, offset(), 1, "The tabs stop at the last one");
    }

    @Test
    void reducedMotionMovesAtOnce() throws Exception {
        reducedMotion.set(true);

        onFx(() -> scroll.scrollBy(150));

        assertEquals(150, offset(), 1);
    }

    @Test
    void anotherScrollTakesOver() throws Exception {
        onFx(() -> scroll.scrollBy(600));
        Thread.sleep(80);

        assertTrue(offset() > 0, "The glide has started");

        onFx(() -> pane.setHvalue(0));
        Thread.sleep(300);

        assertEquals(0, offset(), 1, "The wheel or revealing a tab stops the glide");
    }

    private double offset() throws Exception {
        return JavaFxTestRuntime.call(() -> pane.getHvalue() * OVERFLOW);
    }

    private static void onFx(Runnable action) throws Exception {
        JavaFxTestRuntime.call(() -> {
            action.run();

            return null;
        });
    }
}
