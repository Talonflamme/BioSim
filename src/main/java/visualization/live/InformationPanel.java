package visualization.live;

import org.apache.commons.lang3.tuple.ImmutablePair;
import simulation.Agent;
import simulation.brain.Brain;
import simulation.reports.LastGenerationInfo;
import simulation.reports.Report;
import visualization.BrainVisualizer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static utility.Utility.format;

class InformationPanel extends JPanel {
    private static final int PAD_X = 10;
    private static final int PAD_Y = 10;
    /**
     * The height that {@link BrainVisualizer#HEIGHT} is temporarily assigned to during the
     * draw process of this component.
     */
    private static final int BRAIN_IMAGE_HEIGHT = 630;
    private static final Font H1 = new Font("Poppins", Font.BOLD, 30);
    private static final Font H2 = new Font("Poppins", Font.BOLD, 24);
    private static final Font H3 = new Font("Poppins", Font.PLAIN, 20);
    private static final Font H6 = new Font("Poppins", Font.PLAIN, 13);

    /**
     * <p>
     * Left: the brain that is most common.
     * </p>
     * <p>
     * Right: the count of that brain
     * </p>
     */
    private ImmutablePair<Brain, Integer> mostCommonBrain;
    private BufferedImage brainImage;
    private int gen;
    /**
     * <p>A value from 0 to 1 that represents how diverse the agents are.</p>
     * <p>1 means higher diversity.</p>
     * <p>0 means all are exactly equal.</p>
     */
    private float geneticDiversity;
    /**
     * A value representing the ratio of agents that survived last generation. Probably derived from
     * {@link LastGenerationInfo#surviveRate()}. Always between {@code 0} and {@code 1}. Defaults to
     * {@code 100%} (1).
     * <p>Note: {@code 0} will never occur, since the simulation is cancelled when no agent is alive.</p>
     */
    private float surviveRate;

    final UserInputPanel userInput;

    /**
     * @param leftAvailable   The available space for this component from the left.
     * @param topAvailable    The available space for this component from the top.
     * @param widthAvailable  The available width for this component.
     * @param heightAvailable The available height for this component.
     */
    InformationPanel(int leftAvailable, int topAvailable, int widthAvailable, int heightAvailable, LiveSimulationWindow window) {
        super();

        surviveRate = 1f; // default to 100%

        setLayout(null);
        setBounds(leftAvailable + PAD_X, topAvailable + PAD_Y,
                widthAvailable - PAD_X * 2, heightAvailable - PAD_Y * 2);
        setBackground(new Color(0x5586ff));

        userInput = new UserInputPanel(getWidth() - PAD_X, getHeight() - PAD_Y, window);
        add(userInput);
    }

    private static void drawCenter(Graphics2D g, String str, int xOff, int width, int y) {
        int w = g.getFontMetrics().stringWidth(str);
        g.drawString(str, xOff + (width - w) / 2, y);
    }

    private void drawTextOnImage(Graphics2D g, int imgWidth, int xOff, int yOff) {
        String str1 = "Most Common Gene (%d): ".formatted(mostCommonBrain.right);

        String str2 = mostCommonBrain.left.genomeToString();
        if (str2.length() > 71) { // 71 = length for 8 genes
            str2 = str2.substring(0, 72);
        }

        // draw most common gene text
        g.setFont(H2);
        g.setColor(Color.WHITE);
        drawCenter(g, str1, xOff, imgWidth, yOff + 26);

        // draw genome text
        g.setColor(new Color(0x8855ff));
        g.setFont(H6);
        int str2len = str2.length() / 2;
        drawCenter(g, str2.substring(0, str2len), xOff, imgWidth, yOff + 46);
        drawCenter(g, str2.substring(str2len), xOff, imgWidth, yOff + 60);
    }

    @Override
    protected void paintComponent(Graphics g1d) {
        super.paintComponent(g1d);

        Graphics2D g = (Graphics2D) g1d;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setFont(H1);
        g.setColor(Color.WHITE);
        drawCenter(g, "Gen " + gen, 0, getWidth(), 40);

        g.setFont(H3);
        drawCenter(g, "Genetic Diversity: " + format(geneticDiversity * 100) + "%", 0, getWidth(), 70);

        int imgWidth = getWidth() - 20;
        int imgHeight = (int) ((BRAIN_IMAGE_HEIGHT / (float) BrainVisualizer.WIDTH) * imgWidth);

        // draw survive rate text
        String surviveRateText = "Survive Rate: %d%%".formatted(Math.round(surviveRate * 100f));
        g.setColor(Color.WHITE);
        g.setFont(H3);
        g.drawString(surviveRateText, 120, 122 + imgHeight);

        if (mostCommonBrain == null) return;

        g.drawImage(brainImage, 10, 90, getWidth() - 20, imgHeight, null);

        drawTextOnImage(g, imgWidth, 10, 90);
    }


    /**
     * Repaints the component with updated values based on the parameters.
     *
     * @param gen         the number of generation.
     * @param agents      the newly created agents.
     * @param surviveRate the same value as {@link simulation.reports.LastGenerationInfo#surviveRate()}
     */
    public void onNewGeneration(int gen, Agent[] agents, float surviveRate) {
        this.geneticDiversity = Report.geneticDiversity(agents);
        this.surviveRate = surviveRate;

        var mostCommonGene = Report.mostCommonGene(agents);
        String genome = mostCommonGene.getKey();
        int count = mostCommonGene.getValue();

        if (count > 1) {
            mostCommonBrain = new ImmutablePair<>(Brain.fromString(genome), count);
            BrainVisualizer visualizer = new BrainVisualizer(mostCommonBrain.left, BRAIN_IMAGE_HEIGHT, 10);
            brainImage = visualizer.createImage();

            this.gen = gen;
        }

        this.repaint();
    }
}
