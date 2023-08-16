package simulation.brain;

public interface Node {
    /**
     * Returns the {@link InputNode input node} matching and ID.
     *
     * @param id A 7-bit ID. Used by taking the modulo.
     */
    static InputNode getSource(byte id) {
        return InputNode.INPUT_NODES[id % InputNode.INPUT_NODES.length];
    }

    /**
     * Returns the {@link OutputNode output node} matching the ID.
     *
     * @param id A 7-bit ID. Used by taking the modulo.
     */
    static OutputNode getTarget(byte id) {
        return OutputNode.OUTPUT_NODES[id % OutputNode.OUTPUT_NODES.length];
    }

    /**
     * Get a short, 2-3 character representation of this node.
     */
    String getShortString();
}
