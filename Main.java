import Classes.Simulator;
import Classes.Crossroad;

public class Main {
    public static void main(String[] args) {

        Crossroad crossroad = new Crossroad();
        crossroad.main(args);

        Simulator simulator = new Simulator(crossroad);
        simulator.startSimulation();
    }
}

