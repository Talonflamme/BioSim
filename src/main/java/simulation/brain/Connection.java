package simulation.brain;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class Connection implements Comparable<Connection> {
    private final SourceNode source;
    private final TargetNode target;
    private float weight;

    public Connection(SourceNode source, TargetNode target, float weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public static void optimize(ArrayList<Connection> connections) {
        HashSet<InnerNode> innerNodes = new HashSet<>();
        for (Connection con : connections) {
            if (con.source instanceof InnerNode) {
                innerNodes.add((InnerNode) con.source);
            }
            if (con.target instanceof InnerNode) {
                innerNodes.add((InnerNode) con.target);
            }
        }

        // check which lead to an output
        HashSet<InnerNode> visitedNodes = new HashSet<>(innerNodes.size());

        for (InnerNode inner : innerNodes) {
            visitedNodes.clear();
            if (!hasOutput(inner, connections, visitedNodes)) {
                removeConnections(connections, inner);
            }
        }

        // now that only inner nodes that lead to an output are in innerNodes, we can focus on inputs
        // check which innerNodes do not have an input
        for (InnerNode inner : innerNodes) {
            visitedNodes.clear();
            if (!hasInput(inner, connections, visitedNodes)) {
                removeConnections(connections, inner);
            }
        }

        // now only nodes that have an effect (have output) and are activated (have input) are in connections
        // check overlapping connections, multiply weights
        for (int i = 0; i < connections.size(); i++) {
            Connection con1 = connections.get(i);

            for (int j = i + 1; j < connections.size(); j++) {
                Connection con2 = connections.get(j);

                if (con1.source == con2.source && con1.target == con2.target) {
                    connections.remove(j--);
                    con1.weight *= con2.weight;
                }
            }
        }
        sort(connections);
    }

    private static void sort(ArrayList<Connection> connections) {
        connections.sort(Connection::compareTo);
    }

    private static void removeConnections(ArrayList<Connection> connections, InnerNode node) {
        for (int i = 0; i < connections.size(); i++) {
            Connection con = connections.get(i);
            if (con.target == node || con.source == node) {
                connections.remove(i--);
            }
        }
    }

    private static boolean hasInput(InnerNode node, ArrayList<Connection> connections, HashSet<InnerNode> visited) {
        visited.add(node);
        for (Connection con : connections) {
            if (con.target != node) continue;
            if (con.source instanceof InputNode) {
                return true;
            }
            InnerNode source = (InnerNode) con.source;
            if (!visited.contains(source) && hasInput(source, connections, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOutput(InnerNode node, ArrayList<Connection> connections, HashSet<InnerNode> visited) {
        visited.add(node);
        for (Connection con : connections) {
            if (con.source != node) continue;
            if (con.target instanceof OutputNode) {
                return true;
            }

            InnerNode target = (InnerNode) con.target;
            if (!visited.contains(target) && hasOutput(target, connections, visited)) {
                return true;
            }
        }
        return false;
    }

    public SourceNode source() {
        return source;
    }

    public TargetNode target() {
        return target;
    }

    public float weight() {
        return weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Connection) obj;
        return Objects.equals(this.source, that.source) &&
                Objects.equals(this.target, that.target) &&
                Float.floatToIntBits(this.weight) == Float.floatToIntBits(that.weight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, weight);
    }

    @Override
    public String toString() {
        return "Connection[" +
                "source=" + source + ", " +
                "target=" + target + ", " +
                "weight=" + weight + ']';
    }

    @Override
    public int compareTo(@NotNull Connection o) {
        // reverse, since higher priority should be sorted at beginning of collection
        return o.getPriority() - this.getPriority();
    }

    /**
     * Returns an integer representing the priority of this connection. Higher return value means higher priority.
     * Order:
     * <ol>
     *     <li>{@link InputNode} -> {@link InnerNode} (Value 4)</li>
     *     <li>{@link InnerNode} -> {@link InnerNode} (Value 3)</li>
     *     <li>{@link InputNode} -> {@link OutputNode} (Value 2)</li>
     *     <li>{@link InnerNode} -> {@link OutputNode} (Value 1)</li>
     * </ol>
     */
    public int getPriority() {
        if (target instanceof InnerNode) {
            return source instanceof InputNode ? 4 : 3;
        }
        // target is OutputNode
        return source instanceof InputNode ? 2 : 1;
    }
}
