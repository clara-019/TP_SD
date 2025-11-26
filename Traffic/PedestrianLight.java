package Traffic;

import Utils.RoundRobin;

public class PedestrianLight extends Thread{
    private static final int GREEN_LIGHT_DURATION_MS = 5000;

    private final RoundRobin roundRobin;
    private final int id;

    public PedestrianLight(RoundRobin roundRobin, int id) {
        this.roundRobin = roundRobin;
        this.id = id;
    }

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
