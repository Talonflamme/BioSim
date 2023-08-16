package simulation.brain;

import java.util.Objects;

public class InnerNode implements SourceNode, TargetNode {
    private final int id;

    public InnerNode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String getShortString() {
        return "IN%d".formatted(this.id);
    }

    @Override
    public String toString() {
        return "InnerNode[%d]".formatted(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
