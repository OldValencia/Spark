package to.sparkapp.app.ui.topbar.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Scroll controls occupy their own space, outside the tab viewport. A click glides the tabs by about half the visible
 * strip; holding the arrow keeps them moving.
 */
public final class AiDockScrollArrow extends StackPane {
    /** Part of the visible strip one click moves, so the next tabs come into view without losing your place. */
    private static final double CLICK_SHARE = 0.5;
    private static final double MIN_CLICK_PIXELS = 120;
    private static final double HOLD_PIXELS = 60;
    private static final Duration HOLD_INTERVAL = Duration.millis(80);
    private static final Duration HOLD_DELAY = Duration.millis(350);

    private final Timeline hold;
    private final Timeline delay;

    AiDockScrollArrow(boolean left, DockSmoothScroll scroll) {
        setMinSize(20, 30);
        setPrefSize(20, 30);
        setMaxSize(20, 30);
        setAlignment(Pos.CENTER);
        setCursor(Cursor.HAND);
        getStyleClass().add("dock-scroll-arrow");

        var glyph = new Label(left ? "‹" : "›");
        glyph.setMouseTransparent(true);
        getChildren().add(glyph);
        setAccessibleText(left ? "Previous sites" : "More sites");
        Tooltip.install(this, ShellTooltip.create(getAccessibleText()));

        var direction = left ? -1 : 1;
        hold = new Timeline(new KeyFrame(HOLD_INTERVAL, e -> scroll.scrollBy(direction * HOLD_PIXELS)));
        hold.setCycleCount(Animation.INDEFINITE);
        delay = new Timeline(new KeyFrame(HOLD_DELAY, e -> hold.play()));

        setOnMousePressed(e -> {
            scroll.scrollBy(direction * clickDistance(scroll));
            delay.playFromStart();
            e.consume();
        });
        setOnMouseReleased(e -> {
            cancel();
            e.consume();
        });
        setOnMouseExited(e -> cancel());
        disabledProperty().addListener((o, before, disabled) -> {
            if (disabled) {
                cancel();
            }
        });
    }

    private static double clickDistance(DockSmoothScroll scroll) {
        return Math.max(MIN_CLICK_PIXELS, scroll.visibleWidth() * CLICK_SHARE);
    }

    public void cancel() {
        delay.stop();
        hold.stop();
    }
}
