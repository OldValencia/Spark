package to.sparkapp.app.windows;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

/**
 * Keeps the window's place and size through a minimize, for example after Win + D.
 *
 * <p>Windows moves a minimized window off screen and shrinks it to a small box. Once such a window is hidden, JavaFX
 * keeps those bounds, so the next show would open it at its smallest size. Sizes set while the window is minimized
 * also do not stick: restoring brings back the size it had before.
 */
final class MinimizedWindowBounds {
    /** The position Windows gives every minimized window. */
    private static final double MINIMIZED_POSITION = -32000;

    private final Stage stage;
    /** Where the window should stand once it is no longer minimized. */
    private Rectangle2D intended;

    private MinimizedWindowBounds(Stage stage) {
        this.stage = stage;
    }

    static void install(Stage stage) {
        var bounds = new MinimizedWindowBounds(stage);
        InvalidationListener moved = observable -> bounds.remember();
        stage.xProperty().addListener(moved);
        stage.yProperty().addListener(moved);
        stage.widthProperty().addListener(moved);
        stage.heightProperty().addListener(moved);
        stage.iconifiedProperty().addListener((observable, previous, iconified) -> bounds.iconifiedChanged(iconified));
        stage.showingProperty().addListener((observable, previous, showing) -> {
            if (!showing) {
                bounds.hidden();
            }
        });
    }

    private void iconifiedChanged(boolean iconified) {
        if (iconified) {
            remember();

            return;
        }

        var target = intended;
        intended = null;

        if (target != null) {
            // Windows reports the restored size after this change, so the intended size goes on afterwards.
            Platform.runLater(() -> restoreShown(target));
        }
    }

    private void remember() {
        if (stage.isShowing() && stage.isIconified() && !isMinimizedPlaceholder()) {
            intended = currentBounds();
        }
    }

    private void hidden() {
        if (!stage.isIconified()) {
            return;
        }

        var target = intended;
        // The next show creates a new native window from these values, so it opens restored and in place.
        stage.setIconified(false);

        if (target != null) {
            apply(target);
        }
    }

    private void restoreShown(Rectangle2D target) {
        if (stage.isShowing() && !stage.isIconified() && !target.equals(currentBounds())) {
            apply(target);
        }
    }

    private boolean isMinimizedPlaceholder() {
        // While the window is hidden, one coordinate may already have moved off screen before the other.
        return stage.getX() <= MINIMIZED_POSITION || stage.getY() <= MINIMIZED_POSITION;
    }

    private Rectangle2D currentBounds() {
        return new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    private void apply(Rectangle2D bounds) {
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
    }
}
