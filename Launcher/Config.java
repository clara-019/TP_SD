package Launcher;

import java.awt.Point;

public final class Config {
    private Config() {}

    public static final int TIMER_DELAY_MS = 30;
    public static final int AUTO_STOP_MS = 60_000;

    public static final double NODE_HALF_WIDTH = 44.0;
    public static final double NODE_HALF_HEIGHT = 28.0;
    public static final double QUEUE_SPACING = 34.0;
    public static final double LIGHT_BACKOFF = 10.0;

    public static final long SIGNAL_ARRIVAL_ANIM_MS = 200L;
    public static final long COMPACT_ANIM_MS = 300L;

    public static Point computeTrafficPoint(Point origin, Point dest, int index) {
        if (origin == null || dest == null)
            return new Point(0, 0);
        double dx = dest.x - origin.x;
        double dy = dest.y - origin.y;
        double len = Math.hypot(dx, dy);
        if (len == 0)
            len = 1;
        double ux = dx / len;
        double uy = dy / len;

        double hw = NODE_HALF_WIDTH, hh = NODE_HALF_HEIGHT;
        double tx = (Math.abs(ux) < 1e-6) ? Double.POSITIVE_INFINITY : (hw / Math.abs(ux));
        double ty = (Math.abs(uy) < 1e-6) ? Double.POSITIVE_INFINITY : (hh / Math.abs(uy));
        double t = Math.min(tx, ty);
        double bx = dest.x - ux * t;
        double by = dest.y - uy * t;

        double spacing = QUEUE_SPACING;
        double shift = spacing * index;
        double finalX = bx - ux * shift;
        double finalY = by - uy * shift;
        return new Point((int) Math.round(finalX), (int) Math.round(finalY));
    }
}
