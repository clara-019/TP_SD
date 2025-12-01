package Traffic;

import Utils.RoundRobin;

/**
 * Simple pedestrian light controller.
 * <p>
 * This thread cooperates with a {@link RoundRobin} coordinator to
 * grant a short pedestrian "green" period for the configured turn id. When
 * its turn arrives it prints a console message, waits for the configured
 * green duration, then prints a red message and releases the turn.
 */
public class PedestrianLight extends Thread {
    private static final int GREEN_LIGHT_DURATION_MS = 5000;

    private final RoundRobin roundRobin;
    private final int id;

    /**
     * Create a pedestrian light controller.
     *
     * @param roundRobin the {@link RoundRobin} coordinator used to wait/release
     *                   turns
     * @param id         the identifier used when interacting with the coordinator
     */
    public PedestrianLight(RoundRobin roundRobin, int id) {
        this.roundRobin = roundRobin;
        this.id = id;
    }

    /**
     * Main loop: wait for the coordinator to grant the turn, hold the
     * green interval, then release the turn. Loop runs indefinitely.
     */
    @Override
    public void run() {
        while (true) {
            try {
                roundRobin.esperarTurno(id);
                long greenStartTime = System.currentTimeMillis();
                long greenEndTime = greenStartTime + GREEN_LIGHT_DURATION_MS;
                System.out.println("Pedestrian Light GREEN");
                while (true) {
                    long now = System.currentTimeMillis();
                    if (now >= greenEndTime) {
                        break;
                    }
                }
                System.out.println("Pedestrian Light RED");
                Thread.sleep(200);
                roundRobin.terminarTurno();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
