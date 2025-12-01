package Launcher;

import Vehicle.Vehicle;
import Vehicle.VehicleType;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class VehicleSprite {
    public final String id;
    public final Vehicle vehicle;

    public double x, y;

    private double startX, startY, tx, ty;
    private long startTime;
    private long durationMs;
    private boolean moving = false;
    private boolean removeWhenArrives = false;

    private double angle = 0.0;
    private boolean hasFaceTarget = false;
    private double faceX = 0.0, faceY = 0.0;

    private static final int LENGTH = 28;
    private static final int WIDTH = 14;
    private static final Color CAR_FILL = new Color(30, 144, 255);
    private static final Color TRUCK_FILL = new Color(100, 100, 100);
    private static final Color MOTOR_FILL = new Color(255, 140, 0);
    private static final Color BORDER = new Color(30, 30, 30, 160);

    public VehicleSprite(String id, Vehicle vehicle, double x, double y) {
        this.id = id;
        this.vehicle = vehicle;
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.tx = x;
        this.ty = y;
    }

    public void setTarget(double tx, double ty, long durationMs) {
        this.startX = this.x;
        this.startY = this.y;
        this.tx = tx;
        this.ty = ty;
        this.startTime = System.currentTimeMillis();
        this.durationMs = Math.max(1, durationMs);
        this.moving = true;

        this.angle = Math.atan2(ty - startY, tx - startX);
    }

    public void setFaceTarget(double fx, double fy) {
        this.faceX = fx;
        this.faceY = fy;
        this.hasFaceTarget = true;
    }

    public void clearFaceTarget() {
        this.hasFaceTarget = false;
    }

    public void markForRemoval() {
        this.removeWhenArrives = true;
    }

    public boolean shouldRemoveNow() {
        return removeWhenArrives && !moving;
    }

    public boolean updatePosition() {
        if (!moving) {
            if (hasFaceTarget) {
                double desired = Math.atan2(faceY - y, faceX - x);
                if (Math.abs(angle - desired) > 1e-3) {
                    angle = desired;
                    return true;
                }
            }
            return false;
        }

        long now = System.currentTimeMillis();
        double t = (now - startTime) / (double) durationMs;
        if (t >= 1.0) {
            x = tx;
            y = ty;
            moving = false;
            if (hasFaceTarget) {
                angle = Math.atan2(faceY - y, faceX - x);
            } else {
                angle = Math.atan2(ty - startY, tx - startX);
            }
            return true;
        }

        double e = clamp(t, 0.0, 1.0);
        double newX = startX + (tx - startX) * e;
        double newY = startY + (ty - startY) * e;

        double vx = newX - x;
        double vy = newY - y;
        if (Math.hypot(vx, vy) > 1e-6)
            angle = Math.atan2(vy, vx);

        x = newX;
        y = newY;
        return true;
    }

    private static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    public void draw(Graphics2D g2) {
        VehicleType vt = vehicle.getType();
        if (vt == null)
            vt = VehicleType.CAR;

        Color fill = (vt == VehicleType.TRUCK) ? TRUCK_FILL : (vt == VehicleType.MOTORCYCLE ? MOTOR_FILL : CAR_FILL);

        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);

        g2.setColor(fill);
        g2.fillRoundRect(-LENGTH / 2, -WIDTH / 2, LENGTH, WIDTH, 6, 6);

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(-LENGTH / 2, -WIDTH / 2, LENGTH, WIDTH, 6, 6);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillRect(LENGTH / 6, -WIDTH / 2 + 2, LENGTH / 6, WIDTH - 4);

        g2.setTransform(old);

        Font orig = g2.getFont();
        Font idFont = orig.deriveFont(Font.BOLD, 12f);
        FontMetrics fm = g2.getFontMetrics(idFont);
        int textW = fm.stringWidth(id);
        int bx = (int) Math.round(x - textW / 2.0);
        int by = (int) Math.round(y - WIDTH / 2.0 - 8);

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(bx - 6, by - fm.getAscent(), textW + 12, fm.getHeight(), 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(idFont);
        g2.drawString(id, bx, by);
        g2.setFont(orig);
    }
}