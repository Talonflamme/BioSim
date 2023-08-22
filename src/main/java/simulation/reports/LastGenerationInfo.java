package simulation.reports;

/**
 * Holds information about the last generation.
 * @param genNum The number of the generation.
 * @param surviveRate A value between {@code 0} and {@code 1}, indicating how many agents survived.
 */
public record LastGenerationInfo(int genNum, float surviveRate) {
}
