package simulation.brain;

import org.jetbrains.annotations.NotNull;

import static simulation.Simulation.config;

import simulation.Agent;
import simulation.Direction;
import simulation.Simulation;
import simulation.config.SimulationConfig;
import simulation.action.*;
import simulation.input.*;
import utility.Utility;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import static utility.Utility.Rand;

public class Brain {
    /**
     * The genome of this brain. It is a list of 32-bit integers.
     */
    private final int[] genome;
    private final Connection[] connections;

    private static final float WEIGHT_SCALE = Short.MAX_VALUE / 4f;

    private Brain(int[] genome) {
        this.genome = genome;
        this.connections = decode();
    }

    /**
     * Constructs the brain structure based on {@link Brain#genome}. Each int (gene) defines a single
     * connection between input and output nodes. This happens as follows:
     * <pre>
     * [1][1010101] [1][1001110] [00101110 01001110]
     *  |     |      |     |              |
     *  |     |      |    target ID       |
     *  |     |      |                    |
     *  |     |     target type          weight
     *  |     |
     *  |    source ID
     *  |
     * source type
     *
     * source type:
     *         0: Source is a InputNode
     *         1: Source is a InnerNode
     *
     * source ID: A 7-bit integer that represents the ID of the source node.
     *      Accessed using {{@link InputNode#INPUT_NODES INPUT_NODES} depending on
     *      {@code source type}. Uses the modulo operator to access the array as follows:
     *      {@code array[id % array.length]}
     *
     * target type:
     *         0: Target is a OutputNode
     *         1: Target is a InnerNode
     *
     * target ID: A 7-bit integer that represents the ID of the target node.
     *       Accessed using {@link OutputNode#OUTPUT_NODES OUTPUT_NODES} depending on
     *       {@code target type}. Uses the modulo operator to access the array as follows:
     *       {@code array[id % array.length]}
     *
     * weight: The weight of the connection. Scaled down to be a floating point. Can be negative or positive.
     *       Scaled down by dividing with {@link Brain#WEIGHT_SCALE}
     * </pre>
     */
    private Connection[] decode() {
        ArrayList<Connection> list = new ArrayList<>();
        for (int gene : genome) {
            boolean sourceTypeBit = getSourceTypeBit(gene); // bit 1
            byte sourceID = getSourceID(gene); // bit 2-8

            boolean targetTypeBit = getTargetTypeBit(gene); // bit 9
            byte targetID = getTargetID(gene); // bit 10-16

            short weightShort = getWeightShort(gene); // 17-32
            float weight = weightShort / WEIGHT_SCALE;

            SourceNode source = getSource(sourceTypeBit, sourceID, list);
            TargetNode target = getTarget(targetTypeBit, targetID, list);
            list.add(new Connection(source, target, weight));
        }

        Connection.optimize(list);
        return list.toArray(Connection[]::new);
    }

    private static short getWeightShort(int gene) {
        return (short) (gene & ((1 << 16) - 1));
    }

    private static byte getTargetID(int gene) {
        return (byte) ((gene >> 16) & 0b1111111);
    }

    private static boolean getTargetTypeBit(int gene) {
        return (gene & (1 << 23)) != 0;
    }

    private static byte getSourceID(int gene) {
        return (byte) ((gene >> 24) & 0b1111111);
    }

    private static boolean getSourceTypeBit(int gene) {
        return (gene & (1 << 31)) != 0;
    }

    /**
     * Returns the source node matching the type and ID.
     *
     * @param type A boolean. 0 = source is {@link InputNode}, 1 = source is {@link InnerNode}
     * @param id   A 7-bit ID. Used by taking the modulo.
     */
    private SourceNode getSource(boolean type, byte id, ArrayList<Connection> connections) {
        if (!type) return Node.getSource(id);
        int innerNodeId = id % config().getMaxInnerNeurons();

        return getInnerNode(innerNodeId, connections);
    }

    @NotNull
    private InnerNode getInnerNode(int innerNodeId, ArrayList<Connection> connections) {
        // try to find already existing inner node
        InnerNode inner = null;
        for (Connection connection : connections) {
            if (connection.source() instanceof InnerNode && ((InnerNode) connection.source()).getId() == innerNodeId) {
                inner = (InnerNode) connection.source();
                break;
            }
            if (connection.target() instanceof InnerNode && ((InnerNode) connection.target()).getId() == innerNodeId) {
                inner = (InnerNode) connection.target();
                break;
            }
        }
        if (inner == null) {
            inner = new InnerNode(innerNodeId);
        }
        return inner;
    }

    /**
     * Returns the target node matching the type and ID.
     *
     * @param type A boolean. 0 = target is {@link OutputNode}, 1 target is {@link InnerNode}
     * @param id   A 7-bit ID. Used by taking the modulo.
     */
    private TargetNode getTarget(boolean type, byte id, ArrayList<Connection> connections) {
        if (!type) return Node.getTarget(id);
        int innerNodeId = id % config().getMaxInnerNeurons();

        return getInnerNode(innerNodeId, connections);
    }

    /**
     * Create a new brain
     */
    public static Brain generate() {
        SimulationConfig config = Simulation.activeSimulation.getConfig();
        int[] genome = new int[config.getGenes()];
        for (int i = 0; i < config.getGenes(); i++) {
            genome[i] = Rand.nextInt();
        }
        return new Brain(genome);
    }

    /**
     * Generates a nearly exact copy of this brain. Might flip some bits if one or more mutation occurs.
     *
     * @return the newly generated brain
     */
    public Brain generateChild() {
        int[] childGenome = mutate1(genome);
        return new Brain(childGenome);
    }

    /**
     * Second, improved version of {@link #mutate0}
     */
    private int[] mutate1(int[] genome) {
        int[] result = new int[genome.length];
        for (int i = 0; i < genome.length; i++) {
            result[i] = genome[i];
            if (Rand.nextFloat() >= config().getMutationRate()) continue;
            float rand = Rand.nextFloat(); // 0-1
            if (rand < 0.2f) { // source type
                result[i] ^= 1;
            } else if (rand < 0.4f) { // source id
                result[i] ^= 1 << Rand.nextInt(1, 8);
            } else if (rand < 0.6f) { // target type
                result[i] ^= 1 << 8;
            } else if (rand < 0.8f) { // target id
                result[i] ^= 1 << Rand.nextInt(9, 16);
            } else { // weight
                result[i] ^= 1 << Rand.nextInt(16, 32); // right now we include the first bit (+ or -)
            }
        }
        return result;
    }

    /**
     * First method of mutation.
     *
     * @deprecated Use {@link #mutate1} instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    private int[] mutate0(int[] genome) {
        int[] result = new int[genome.length];

        for (int i = 0; i < genome.length; i++) {
            result[i] = genome[i];
            if (Rand.nextFloat() >= config().getMutationRate()) continue;
            // flip random bit
            int bitIndex = Rand.nextInt(32);
            result[i] ^= 1 << bitIndex;
        }
        return result;
    }

    public String genomeToString() {
        StringBuilder sb = new StringBuilder();
        for (int gene : genome) {
            sb.append(String.format("%08X ", gene));
        }
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1); // remove trailing whitespace
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Brain[\"" + genomeToString() + "\"]";
    }

    public Color getUniqueColor() {
        int lastGene = genome[genome.length - 1];
        int r = ((genome.length & 1)
                 | ((getSourceTypeBit(genome[0]) ? 1 : 0) << 1)
                 | ((getSourceTypeBit(lastGene) ? 1 : 0) << 2)
                 | ((getTargetTypeBit(genome[0]) ? 1 : 0) << 3)
                 | ((getTargetTypeBit(lastGene) ? 1 : 0) << 4)
                 | ((getSourceID(genome[0]) & 1) << 5)
                 | ((getTargetID(genome[0]) & 1) << 6)
                 | ((getSourceID(lastGene) & 1) << 7));
        int g = ((r & 0b11111) << 3);
        int b = ((r & 0b111) << 5);
        return new Color(r, g, b);
    }

    public Connection[] getConnections() {
        return connections;
    }

    /**
     * Convert the given probability to a boolean by generating a Random number using {@link Utility#Rand Rand}
     * @param probability The probability given as a float of range 0 to 1.
     * @return true with the given probability. false otherwise.
     */
    private static boolean prob2bool(float probability) {
        assert probability >= 0f && probability <= 1f;
        return Rand.nextFloat() < probability;
    }


    /**
     * Performs the forward propagation of the neural network.
     * @param agent the agent to which this brain belongs.
     * @return the actionValues for each {@link ActionType} as an array.
     */
    private float[] feedForward(Agent agent) {
        // first compute all input values
        HashMap<InputNode, Float> inputValues = new HashMap<>();
        ArrayList<InnerNode> innerNodes = new ArrayList<>();
        for (Connection con : connections) {
            if (con.source() instanceof InnerNode inner) {
                innerNodes.add(inner);
            } else if (con.source() instanceof InputNode input) {
                float value = Sensors.getValue(agent, input.getType());
                inputValues.put(input, value);
            }
        }
        float[] actions = new float[OutputNode.OUTPUT_NODES.length];
        float[] innerNeuronsSum = new float[innerNodes.size()];
        boolean innerNodesComputed = false;

        for (Connection con : connections) {
            if (con.target() instanceof OutputNode && !innerNodesComputed) {
                for (int i = 0; i < innerNeuronsSum.length; i++) {
                    innerNeuronsSum[i] = (float) Math.tanh(innerNeuronsSum[i]);
                }
                innerNodesComputed = true;
            }

            if (con.source() instanceof InputNode inputNode) {
                float inputValue = inputValues.get(inputNode);

                if (con.target() instanceof InnerNode innerNode) {
                    innerNeuronsSum[innerNodes.indexOf(innerNode)] += inputValue * con.weight();
                } else {
                    ActionType type = ((OutputNode) con.target()).getType();
                    actions[type.ordinal()] += inputValue * con.weight();
                }
            } else {
                InnerNode innerSource = (InnerNode) con.source();
                int sourceIndex = innerNodes.indexOf(innerSource);

                if (con.target() instanceof InnerNode innerTarget) {
                    int targetIndex = innerNodes.indexOf(innerTarget);
                    innerNeuronsSum[targetIndex] += innerNeuronsSum[sourceIndex] * con.weight();
                } else {
                    ActionType type = ((OutputNode) con.target()).getType();
                    actions[type.ordinal()] += innerNeuronsSum[sourceIndex] * con.weight();
                }
            }
        }

        return actions;
    }

    /**
     * Compute a list of output actions. This is done by firstly getting the input values of
     * {@link InputNode inputs} and computing the values for the {@link OutputNode outputs}.
     * Stores actions and values inside {@link simulation.action.Action Action}
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void think(Agent agent) {
        float[] actionValues = feedForward(agent);

        if (ActionType.DoNothing.isEnabled()) {
            // do nothing
        }

        // movement
        float moveX = ActionType.MoveX.isEnabled() ? actionValues[ActionType.MoveX.ordinal()] : 0f;
        float moveY = ActionType.MoveY.isEnabled() ? actionValues[ActionType.MoveY.ordinal()] : 0f;

        if (ActionType.MoveForward.isEnabled()) {
            moveX += agent.getForward().getX() * actionValues[ActionType.MoveForward.ordinal()];
            moveY += agent.getForward().getY() * actionValues[ActionType.MoveForward.ordinal()];
        }

        if (ActionType.MoveReverse.isEnabled()) {
            moveX += agent.getForward().reverse().getX() * actionValues[ActionType.MoveReverse.ordinal()];
            moveY += agent.getForward().reverse().getY() * actionValues[ActionType.MoveReverse.ordinal()];
        }

        if (ActionType.MoveRand.isEnabled()) {
            Direction dir = Direction.random();
            moveX += dir.getX() * actionValues[ActionType.MoveRand.ordinal()];
            moveY += dir.getY() * actionValues[ActionType.MoveRand.ordinal()];
        }

        moveX = (float) Math.tanh(moveX);
        moveY = (float) Math.tanh(moveY);

        boolean hasX = prob2bool(Math.abs(moveX));
        boolean hasY = prob2bool(Math.abs(moveY));

        int x = (int) Math.copySign(hasX ? 1 : 0, moveX);
        int y = (int) Math.copySign(hasY ? 1 : 0, moveY);
        Direction newForward = Direction.get(x, y);
        agent.queueMove(newForward);
    }
}
