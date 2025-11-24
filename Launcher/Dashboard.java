// =========================================================
//  DASHBOARD REFEITO: Melhor estrutura + melhor visual
//  (versão com tratamento completo de EventType)
// =========================================================

package Launcher;

import Event.Event;
import Event.EventType;
import Event.SignalChangeEvent;
import Event.VehicleEvent;
import Node.NodeEnum;
import Node.RoadEnum;
import Node.NodeType;
import Vehicle.Vehicle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class Dashboard extends JFrame {

    private Simulator simulator;
    private PriorityBlockingQueue<Event> eventQueue;
    private Thread eventConsumer;

    private final Map<String, VehicleSprite> sprites = new HashMap<>();
    private final Map<NodeEnum, Point> nodePositions = new HashMap<>();
    private final Map<RoadEnum, String> trafficLights = new HashMap<>();

    private JTextArea logArea;
    private JLabel statusLabel;
    private DashboardRenderer renderer;

    public Dashboard() {
        super("Traffic Simulator Dashboard - Enhanced");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 900);
        setLayout(new BorderLayout());

        initNodes();

        renderer = new DashboardRenderer(nodePositions, sprites, trafficLights);
        add(renderer, BorderLayout.CENTER);

        // =====================================
        // TOP PANEL
        // =====================================
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(34, 40, 49));
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton startBtn = makeButton("Start");
        JButton stopBtn = makeButton("Stop");

        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> stopSimulation());

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(startBtn);
        controls.add(stopBtn);

        top.add(controls, BorderLayout.WEST);

        statusLabel = new JLabel("STOPPED");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.RED);
        top.add(statusLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // =====================================
        // LOG PANEL
        // =====================================
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        // =====================================
        // ANIMATION TIMER
        // =====================================
        new Timer(30, e -> {
            boolean changed = false;
            synchronized (sprites) {
                for (Iterator<Map.Entry<String, VehicleSprite>> it = sprites.entrySet().iterator(); it.hasNext(); ) {
                    VehicleSprite s = it.next().getValue();
                    if (s.updatePosition()) changed = true;
                    if (s.shouldRemoveNow()) {
                        it.remove();
                        changed = true;
                    }
                }
            }
            if (changed) renderer.repaint();
        }).start();
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(new Color(48, 71, 94));
        b.setForeground(Color.WHITE);
        b.setFocusable(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private void initNodes() {
    int cellW = 300;
    int cellH = 200;

    Map<NodeEnum, Point> grid = new LinkedHashMap<>();

    // Coluna 0: Entradas
    grid.put(NodeEnum.E1, new Point(0 * cellW + cellW/2, 0 * cellH + cellH/2));
    grid.put(NodeEnum.E2, new Point(0 * cellW + cellW/2, 1 * cellH + cellH/2));
    grid.put(NodeEnum.E3, new Point(0 * cellW + cellW/2, 2 * cellH + cellH/2));

    // Coluna 1: Crossroads centrais
    grid.put(NodeEnum.CR1, new Point(1 * cellW + cellW/2, 0 * cellH + cellH/2));
    grid.put(NodeEnum.CR2, new Point(1 * cellW + cellW/2, 1 * cellH + cellH/2));
    grid.put(NodeEnum.CR3, new Point(1 * cellW + cellW/2, 2 * cellH + cellH/2));

    // Coluna 2: Crossroads + saída
    grid.put(NodeEnum.CR4, new Point(2 * cellW + cellW/2, 0 * cellH + cellH/2));
    grid.put(NodeEnum.CR5, new Point(2 * cellW + cellW/2, 1 * cellH + cellH/2));
    grid.put(NodeEnum.S,   new Point(2 * cellW + cellW/2, 2 * cellH + cellH/2));

    nodePositions.clear();
    nodePositions.putAll(grid);

    // Inicializar semáforos das roads
    trafficLights.clear();
    for (RoadEnum r : RoadEnum.values()) {
        if (r.getDestination().getType() == NodeType.CROSSROAD) {
            trafficLights.put(r, "RED");
        }
    }
}

    private void startSimulation() {
        if (simulator != null && simulator.isRunning()) {
            log("Simulator already running");
            return;
        }

        simulator = new Simulator();
        eventQueue = simulator.getEventQueue();

        Thread simThread = new Thread(simulator::startSimulation);
        simThread.setDaemon(true);
        simThread.start();

        statusLabel.setText("RUNNING");
        statusLabel.setForeground(Color.GREEN);

        eventConsumer = new Thread(() -> {
            try {
                // Protegemos contra NPE: verifica simulator != null também
                while (simulator != null && simulator.isRunning()) {
                    Event ev = eventQueue.take(); // bloqueia até chegar evento
                    handleEvent(ev);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ex) {
                // Log inesperado para debugging
                log("Event consumer crashed: " + ex.getMessage());
            }
        });
        eventConsumer.setDaemon(true);
        eventConsumer.start();

        log("Simulator started");
    }

    private void stopSimulation() {
        if (simulator != null) simulator.stopSimulation();
        if (eventConsumer != null) eventConsumer.interrupt();

        statusLabel.setText("STOPPED");
        statusLabel.setForeground(Color.RED);

        // Limpamos a fila de eventos (se existir)
        if (eventQueue != null) {
            while ((eventQueue.poll()) != null) {
                // opcional: log de descarte
            }
        }

        synchronized (sprites) {
            sprites.clear();
        }

        renderer.repaint();
        log("Simulator stopped");
    }

    private void handleEvent(Event ev) {
        if (ev == null) return;
        log(ev.toString());

        // TRAFFIC LIGHT CHANGES
        if (ev instanceof SignalChangeEvent s) {
            // Se evento especificar estrada, atualiza só essa.
            if (s.getRoad() != null) {
                trafficLights.put(s.getRoad(), s.getSignalColor());
            } else {
                // Fallback: atualiza todas as estradas que chegam ao cruzamento indicado
                for (RoadEnum r : RoadEnum.getRoadsToCrossroad(s.getNode())) {
                    trafficLights.put(r, s.getSignalColor());
                }
            }
            SwingUtilities.invokeLater(renderer::repaint);
            return;
        }

        // Veículo relacionado a eventos
        if (!(ev instanceof VehicleEvent ve)) {
            // Tipo de evento desconhecido para esta camada (poderá existir noutros módulos)
            log("Evento não processado pelo Dashboard: " + ev.getClass().getSimpleName());
            return;
        }

        Vehicle v = ve.getVehicle();
        if (v == null) {
            log("VehicleEvent sem veículo associado");
            return;
        }
        String id = v.getId();

        EventType type = ve.getType();
        if (type == null) {
            log("VehicleEvent sem tipo definido");
            return;
        }

        switch (type) {

            case NEW_VEHICLE -> {
                // cria sprite parado no nó de origem (ou fallback para centro do painel)
                Point p = nodePositions.get(ve.getNode());
                if (p == null) {
                    // fallback: coloca no centro do painel (evita NPEs)
                    p = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                    nodePositions.put(ve.getNode(), p);
                }
                synchronized (sprites) {
                    if (!sprites.containsKey(id)) {
                        sprites.put(id, new VehicleSprite(id, v, p.x, p.y));
                    } else {
                        // Se já existir, atualiza posição inicial
                        VehicleSprite s = sprites.get(id);
                        s.x = p.x; s.y = p.y;
                    }
                }
            }

            case VEHICLE_DEPARTURE -> {
                // Inicia movimento entre nós
                handleDeparture(ve, v);
            }

            case VEHICLE_ARRIVAL -> {
                // Snap ao nó (posição final)
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(id);
                    Point p = nodePositions.get(ve.getNode());
                    if (s == null) {
                        // Se não existir sprite (evento desordenado), cria e posiciona
                        if (p == null) {
                            p = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                            nodePositions.put(ve.getNode(), p);
                        }
                        sprites.put(id, new VehicleSprite(id, v, p.x, p.y));
                    } else if (p != null) {
                        s.x = p.x;
                        s.y = p.y;
                    }
                }
            }

            case VEHICLE_EXIT -> {
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(id);
                    if (s != null) s.markForRemoval();
                    else {
                        // se não existe sprite, nada a fazer
                    }
                }
            }

            default -> {
                // EventType adicionado futuramente? Log para debug
                log("Tipo de VehicleEvent não tratado: " + type);
            }
        }

        SwingUtilities.invokeLater(renderer::repaint);
    }

    private void handleDeparture(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        synchronized (sprites) {
            VehicleSprite s = sprites.get(id);

            // se sprite não existe (evento fora de ordem), criamos no nó de origem
            if (s == null) {
                Point originPos = nodePositions.get(ve.getNode());
                if (originPos == null) {
                    originPos = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                    nodePositions.put(ve.getNode(), originPos);
                }
                s = new VehicleSprite(id, v, originPos.x, originPos.y);
                sprites.put(id, s);
            }

            NodeEnum next = findNextNode(v, ve.getNode());
            if (next == null) {
                // sem próximo nó (talvez seja saída direta) — marca remoção segura
                s.markForRemoval();
                return;
            }

            RoadEnum road = findRoad(ve.getNode(), next);
            if (road == null) {
                // não encontrou estrada; faz snap e marca remoção (evita bloqueio visual)
                Point destP = nodePositions.get(next);
                if (destP != null) {
                    s.setTarget(destP.x, destP.y, 500);
                } else {
                    s.markForRemoval();
                }
                return;
            }

            Point dest = nodePositions.get(next);
            if (dest == null) {
                // fallback para centro
                dest = new Point(renderer.getWidth()/2, renderer.getHeight()/2);
                nodePositions.put(next, dest);
            }

            long base = v.getType().getTimeToPass(road.getTime());
            long anim = (long) (base * 2.5); // factor de animação para visual mais lento/agradavel
            s.setTarget(dest.x, dest.y, anim);
        }
    }

    private NodeEnum findNextNode(Vehicle v, NodeEnum current) {
        if (v.getPath() == null) return null;

        List<NodeEnum> list = v.getPath().getPath();
        for (int i = 0; i < list.size() - 1; i++)
            if (list.get(i) == current)
                return list.get(i + 1);

        return null;
    }

    private RoadEnum findRoad(NodeEnum a, NodeEnum b) {
        for (RoadEnum r : RoadEnum.values())
            if (r.getOrigin() == a && r.getDestination() == b)
                return r;
        return null;
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(s + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}
