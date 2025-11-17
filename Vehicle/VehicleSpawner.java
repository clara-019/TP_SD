package Vehicle;

import Utils.SynchronizedQueue;
import java.util.Random;
import java.time.Instant;

/**
 * Thread responsável por criar veículos aleatórios periodicamente
 * Gera diferentes tipos de veículos com percursos aleatórios
 */
public class VehicleSpawner extends Thread {
    private SynchronizedQueue<Vehicle> vehiclesToSend;
    private volatile boolean running;
    private int spawnIntervalMs;

    public VehicleSpawner(SynchronizedQueue<Vehicle> vehiclesToSend, boolean running, int spawnIntervalMs) {
        this.vehiclesToSend = vehiclesToSend;
        this.running = running;
        this.spawnIntervalMs = spawnIntervalMs;
    }

    @Override
    public void run() {
        int vehicleCounter = 0;
        Random random = new Random();

        System.out.println("[VehicleSpawner] Iniciando gerador de veículos");

        while(running){
            try {
                // Cria um novo veículo com ID único
                Vehicle newVehicle = createNewVehicle("V" + vehicleCounter, random);
                vehiclesToSend.add(newVehicle);

                System.out.println("[VehicleSpawner] Veículo criado: " + newVehicle.getId() +
                        " Tipo: " + newVehicle.getType() +
                        " Percurso: " + newVehicle.getPath());

                vehicleCounter++;
                Thread.sleep(spawnIntervalMs);

            } catch (InterruptedException e) {
                System.out.println("[VehicleSpawner] Interrompido");
                running = false;
            }
        }

        System.out.println("[VehicleSpawner] Terminado. Total de veículos criados: " + vehicleCounter);
    }

    /**
     * Cria um novo veículo com características aleatórias
     * @param id Identificador único do veículo
     * @param random Gerador de números aleatórios
     * @return Novo veículo criado
     */
    private Vehicle createNewVehicle(String id, Random random){
        // Seleciona tipo aleatório
        VehicleTypes[] types = VehicleTypes.values();
        VehicleTypes type = types[random.nextInt(types.length)];

        // Seleciona percurso aleatório
        PathEnum[] paths = PathEnum.values();
        PathEnum path = paths[random.nextInt(paths.length)];

        // Cria o veículo com timestamp de entrada
        Vehicle vehicle = new Vehicle(id, type, path);
        vehicle.setEntranceTime((int) (Instant.now().getEpochSecond() & Integer.MAX_VALUE));

        return vehicle;
    }

    /**
     * Para a geração de veículos
     */
    public void stopSpawning() {
        this.running = false;
        this.interrupt();
    }
}