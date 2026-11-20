package to.sparkapp.app.ui.topbar.components;

import java.util.function.BooleanSupplier;

import javafx.animation.AnimationTimer;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/**
 * Glides the tabs toward a target position, so the scroll arrows move them smoothly and quick clicks add up. Any other
 * scroll, such as the wheel or revealing the open site, takes over at once.
 */
final class DockSmoothScroll {
    /** How long the tabs take to cover most of the remaining distance; shorter feels snappier. */
    private static final double EASING_SECONDS = 0.07;
    private static final double SETTLED_PIXELS = 0.5;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final ScrollPane pane;
    private final Region content;
    private final BooleanSupplier reducedMotion;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            frame(now);
        }
    };
    private double target;
    private long lastFrame;
    private boolean running;
    private boolean scrolling;

    DockSmoothScroll(ScrollPane pane, Region content, BooleanSupplier reducedMotion) {
        this.pane = pane;
        this.content = content;
        this.reducedMotion = reducedMotion;
        pane.hvalueProperty().addListener(observable -> {
            if (!scrolling) {
                stop();
            }
        });
    }

    /** Moves the tabs by the given distance, counted from where an earlier glide was heading. */
    void scrollBy(double pixels) {
        var overflow = overflow();

        if (overflow <= 0) {
            return;
        }

        var start = running ? target : offset(overflow);
        target = Math.clamp(start + pixels, 0, overflow);

        if (reducedMotion.getAsBoolean()) {
            stop();
            moveTo(target, overflow);

            return;
        }

        if (!running) {
            running = true;
            lastFrame = 0;
            timer.start();
        }
    }

    double visibleWidth() {
        return pane.getViewportBounds().getWidth();
    }

    void stop() {
        timer.stop();
        running = false;
    }

    private void frame(long now) {
        var seconds = lastFrame == 0 ? 1 / 60.0 : (now - lastFrame) / NANOS_PER_SECOND;
        lastFrame = now;

        var overflow = overflow();
        target = Math.clamp(target, 0, Math.max(0, overflow));
        var current = offset(overflow);
        var remaining = target - current;

        if (overflow <= 0 || Math.abs(remaining) <= SETTLED_PIXELS) {
            moveTo(target, overflow);
            stop();

            return;
        }

        moveTo(current + remaining * (1 - Math.exp(-seconds / EASING_SECONDS)), overflow);
    }

    private double overflow() {
        return content.getWidth() - visibleWidth();
    }

    private double offset(double overflow) {
        return pane.getHvalue() * overflow;
    }

    private void moveTo(double offset, double overflow) {
        if (overflow <= 0) {
            return;
        }

        scrolling = true;
        pane.setHvalue(Math.clamp(offset / overflow, 0, 1));
        scrolling = false;
    }
}
