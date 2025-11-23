package Launcher;

import Vehicle.Vehicle;
import Vehicle.VehicleTypes;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * VehicleSprite: representa o estado de desenho/animacao de um veículo.
 * - suporta animação com easing (cubic ease-in-out)
 * - orienta o sprite conforme a direção do movimento (retângulo girado)
 * - desenha o ID do veículo sobre um fundo semi-opaço para legibilidade
 */
public class VehicleSprite {
    public final String id;
    public final Vehicle vehicle;

    // current position
    public double x, y;

    // animation start and target
    private double startX, startY, tx, ty;
    private long startTime;
    private long durationMs;
    private boolean moving = false;
    private boolean removeWhenArrives = false;

    // orientation (radians) used for drawing
    private double angle = 0.0;

    public VehicleSprite(String id, Vehicle vehicle, double x, double y) {
        this.id = id;
        this.vehicle = vehicle;
        this.x = x; this.y = y;
        this.startX = x; this.startY = y; this.tx = x; this.ty = y;
    }

    /**
     * Define o próximo alvo de animação e a duração em ms.
     */
    public void setTarget(double tx, double ty, long durationMs) {
        this.startX = this.x; this.startY = this.y;
        this.tx = tx; this.ty = ty;
        this.startTime = System.currentTimeMillis();
        this.durationMs = Math.max(1, durationMs);
        this.moving = true;

        // compute base angle toward target immediately (used while moving)
        this.angle = Math.atan2(ty - startY, tx - startX);
    }

    public void markForRemoval() { this.removeWhenArrives = true; }
    public boolean shouldRemoveNow() { return removeWhenArrives && !moving; }

    /**
     * Atualiza a posição do sprite. Retorna true se houve mudança (necessita repaint).
     */
    public boolean updatePosition() {
        if (!moving) return false;

        long now = System.currentTimeMillis();
        double t = (now - startTime) / (double) durationMs;
        if (t >= 1.0) {
            x = tx; y = ty; moving = false; return true;
        }

        double e = easeInOutCubic(clamp(t, 0.0, 1.0));
        double newX = startX + (tx - startX) * e;
        double newY = startY + (ty - startY) * e;

        // compute instantaneous angle based on velocity vector
        double vx = newX - x; double vy = newY - y;
        if (Math.hypot(vx, vy) > 1e-6) angle = Math.atan2(vy, vx);

        x = newX; y = newY;
        return true;
    }

    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }

    /**
     * Cubic ease-in-out: smooth acceleration then deceleration
     */
    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    public void draw(Graphics2D g2) {
        // choose appearance based on vehicle type
        VehicleTypes vt = vehicle.getType();
        if (vt == null) vt = VehicleTypes.CAR;

        int length = 28; // along motion
        int width = 14;  // across motion

        Color fill = switch (vt) {
            case CAR -> new Color(30, 144, 255);
            case TRUCK -> new Color(100, 100, 100);
            case MOTORCYCLE -> new Color(255, 140, 0);
            default -> new Color(200, 200, 200);
        };

        // prepare transform for rotation
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);

        // draw body (rounded rectangle centered at 0,0)
        g2.setColor(fill);
        g2.fillRoundRect(-length/2, -width/2, length, width, 6, 6);

        // subtle border
        g2.setColor(new Color(30,30,30,160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(-length/2, -width/2, length, width, 6, 6);

        // small directional detail (windshield)
        g2.setColor(new Color(255,255,255,90));
        g2.fillRect(length/6, -width/2 + 2, length/6, width - 4);

        // restore transform
        g2.setTransform(old);

        // ID label above the vehicle
        Font orig = g2.getFont();
        Font idFont = orig.deriveFont(Font.BOLD, 12f);
        FontMetrics fm = g2.getFontMetrics(idFont);
        int textW = fm.stringWidth(id);
        int textH = fm.getHeight();
        int bx = (int) Math.round(x - textW/2.0);
        int by = (int) Math.round(y - width/2.0 - 8);

        g2.setColor(new Color(0,0,0,150));
        g2.fillRoundRect(bx - 6, by - fm.getAscent(), textW + 12, textH, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(idFont);
        g2.drawString(id, bx, by);
        g2.setFont(orig);
    }
}
