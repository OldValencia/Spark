package to.sparkapp.app.windows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import to.sparkapp.app.support.JavaFxTestRuntime;

/** Minimizes a real window, as Win + D does, and checks that it comes back with the size it should have. */
@Tag("native")
@Timeout(30)
class MinimizedWindowBoundsTest {
    private static final Rectangle2D CUSTOM = new Rectangle2D(300, 200, 913, 677);
    private static final long SETTLE_MILLIS = 600;

    private Stage stage;

    @BeforeEach
    void showWindow() throws Exception {
        stage = JavaFxTestRuntime.call(() -> {
            var window = new Stage(StageStyle.TRANSPARENT);
            window.setScene(new Scene(new StackPane(), Color.TRANSPARENT));
            window.setX(CUSTOM.getMinX());
            window.setY(CUSTOM.getMinY());
            window.setWidth(CUSTOM.getWidth());
            window.setHeight(CUSTOM.getHeight());
            MinimizedWindowBounds.install(window);
            window.show();

            return window;
        });
        Thread.sleep(SETTLE_MILLIS);
    }

    @AfterEach
    void closeWindow() throws Exception {
        onFx(stage::close);
    }

    @Test
    void aWindowHiddenWhileMinimizedOpensAgainWithItsSize() throws Exception {
        onFx(() -> stage.setIconified(true));
        onFx(stage::hide);
        onFx(stage::show);

        assertFalse(JavaFxTestRuntime.call(stage::isIconified), "The window opens restored");
        assertEquals(CUSTOM, bounds(), "The window keeps its place and size");
    }

    @Test
    void aSizeChosenWhileMinimizedStaysAfterRestoring() throws Exception {
        var chosen = new Rectangle2D(CUSTOM.getMinX(), CUSTOM.getMinY(), 700, 500);

        onFx(() -> stage.setIconified(true));
        onFx(() -> {
            stage.setWidth(chosen.getWidth());
            stage.setHeight(chosen.getHeight());
        });
        onFx(() -> stage.setIconified(false));

        assertEquals(chosen, bounds(), "The size chosen while minimized is kept");
    }

    private Rectangle2D bounds() throws Exception {
        return JavaFxTestRuntime.call(
                () -> new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));
    }

    private static void onFx(Runnable action) throws Exception {
        JavaFxTestRuntime.call(() -> {
            action.run();

            return null;
        });
        Thread.sleep(SETTLE_MILLIS);
    }
}
