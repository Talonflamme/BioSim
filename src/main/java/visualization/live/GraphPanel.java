package visualization.live;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.function.Function;

import static utility.Utility.*;

class GraphPanel extends JPanel {
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color AXIS_COLOR = Color.BLACK;
    private static final Color SURVIVE_RATE_COLOR = new Color(251, 61, 93);
    private static final Color GENETIC_DIVERSITY_COLOR = new Color(0, 145, 255);
    private static final Font H5 = new Font("Poppins", Font.PLAIN, 12);
    private static final Font H6 = new Font("Poppins", Font.PLAIN, 10);

    /**
     * Holds tuples that hold information about their generation.
     * Tuples are in the form (SurviveRate, GeneticDiversity) (float, float)
     */
    private final ArrayList<DataPoint> data;

    GraphPanel(InformationPanel parent) {
        int y = InformationPanel.IMG_Y + parent.getImgHeight() + 12;
        int height = parent.getHeight() - y - 12;
        setBounds(InformationPanel.IMG_PAD_X, y, parent.getImgWidth(), height);
        setBackground(TRANSPARENT);
        data = new ArrayList<>();
    }

    /**
     * Adds information to this graph. Also causes a repaint.
     * Automatically increases the generation size without checking it.
     *
     * @param surviveRate      the survive rate of the generation. Should be between 0 and 1.
     * @param geneticDiversity the genetic diversity of the generation. Should be between 0 and 1.
     */
    void addData(float surviveRate, float geneticDiversity) {
        data.add(new DataPoint(surviveRate, geneticDiversity));
    }

    /**
     * @return the generation that number that is the last displayed generation.
     */
    private int maxGen() {
        return data.size() - 1;
    }

    /**
     * @param gen the generation for which to get the x coordinate. Should be greater or equal to 0.
     * @return the (normalized) x coordinate for the provided generation number. This is a value between 0 and 1.
     */
    private float getXForGen(int gen) {
        if (gen == 0) return 0;
        float per = (float) gen / maxGen();
        return Math.min(per, 0.2f * gen); // we Math.min to 0.2, since the graph looks bad when very few generations are displayed
    }

    private int getLeft() {
        return (int) (getWidth() * 0.14);
    }

    private int getRight() {
        return (int) (getWidth() * 0.9);
    }

    private int getTop() {
        return (int) (getHeight() * 0.1);
    }

    private int getBottom() {
        return (int) (getHeight() * 0.9);
    }

    private void drawAxes(Graphics2D g) {
        g.setColor(AXIS_COLOR);

        int left = getLeft();
        int right = getRight();
        int top = getTop();
        int bottom = getBottom();
        int width = right - left;
        int height = bottom - top;

        // horizontal
        g.drawLine(left, bottom, right, bottom);

        // horizontal axis text
        g.drawString("Gen", right + 2f, bottom + H5.getSize() / 2.5f);

        // vertical
        g.drawLine(left, top, left, bottom);

        // horizontal indicators
        final int NUM_INDICATORS = 5;
        for (int i = 0; i < NUM_INDICATORS; i++) {
            float eachHeight = height / (NUM_INDICATORS - 1f);
            float y = bottom - eachHeight * i;

            Line2D line = new Line2D.Float(left - 8, y, left, y);
            g.draw(line);

            String text = format(100 / (NUM_INDICATORS - 1f) * i) + "%";
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D stringBounds = fm.getStringBounds(text, g);

            g.drawString(text, left - 10 - (float) stringBounds.getWidth(), y + H5.getSize() / 2.5f);
        }

        // vertical indicators
        for (int i = 0; i <= maxGen(); i++) {
            if (!shouldIndicateGen(i)) continue;

            float x = getXForGen(i) * width + left;

            Line2D line = new Line2D.Float(x, bottom, x, bottom + 8);
            g.draw(line);

            String text = String.valueOf(i);
            FontMetrics fm = g.getFontMetrics();
            int strWidth = fm.stringWidth(text);
            g.drawString(text, x - strWidth / 2f, bottom + fm.getHeight() + 2f);
        }
    }


    /**
     * @param gen the generation number. Minimum of 0.
     * @return whether the indicator of the gen should be displayed. This basically decides whether to draw the number
     * for indicating the value.
     */
    private boolean shouldIndicateGen(int gen) {
        int mg = maxGen();
        if (mg <= 8 || gen == 0 || gen == mg) return true;
        float factor = mg / 8f;
        for (int i = 1; i <= 8; i++) {
            if (Math.round(factor * i) == gen) {
                return true;
            }
        }
        return false;
    }

    private float calcRadius() {
        int mg = maxGen();
        if (mg <= 32) {
            return 3f;
        }
        return Math.max(3f - (mg - 32f) * 0.04f, 0f);
    }

    private void drawData(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create(); // create new context, because we change some variables, but do not revert them
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        DataPoint last = null;
        int lastI = -1;

        int left = getLeft();
        int top = getTop();
        int right = getRight();
        int bottom = getBottom();
        int width = right - left;
        int height = bottom - top;

        float r = calcRadius();

        // input: y field (0 to 1)
        // output: y center coordinate
        Function<Float, Float> getCY = value -> (1f - value) * height + top;
        // input: generation number
        // output: x center coordinate
        Function<Integer, Float> getCX = gen -> getXForGen(gen) * width + left;

        for (int i = 0; i < data.size(); i++) {
            DataPoint dp = data.get(i);
            float cx = getCX.apply(i);

            // survive rate
            float cy = getCY.apply(dp.surviveRate);
            g.setColor(SURVIVE_RATE_COLOR);
            Ellipse2D.Float ellipse = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
            g.fill(ellipse);
            // connect to last point
            g.setStroke(new BasicStroke(0.3f));
            if (last != null) {
                Line2D line = new Line2D.Float(cx, cy, getCX.apply(lastI), getCY.apply(last.surviveRate));
                g.draw(line);
            }

            // genetic diversity
            cy = getCY.apply(dp.geneticDiversity);
            g.setColor(GENETIC_DIVERSITY_COLOR);
            ellipse.y = cy - r;
            g.fill(ellipse);
            // connect to last point
            if (last != null) {
                Line2D line = new Line2D.Float(cx, cy, getCX.apply(lastI), getCY.apply(last.geneticDiversity));
                g.draw(line);
            }

            last = dp;
            lastI = i;
        }

        g.dispose();
    }

    private void drawLegend(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();

        g.setFont(H6);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0xD5D7D8));
        final int x = getLeft() + 10;
        final int y = 6;
        g.fillRoundRect(x, y, 115, 62, 10, 10);

        // survive rate
        g.setColor(SURVIVE_RATE_COLOR);
        g.fillOval(x + 9, y + 16, 6, 6);
        g.setColor(Color.BLACK);
        g.drawString("Survive Rate", x + 19, y + 22);

        // genetic diversity
        g.setColor(GENETIC_DIVERSITY_COLOR);
        g.fillOval(x + 9, y + 36, 6, 6);
        g.setColor(Color.BLACK);
        g.drawString("Genetic Diversity", x + 19, y + 42);

        g.dispose();
    }

    @Override
    protected void paintComponent(Graphics g1d) {
        super.paintComponent(g1d);

        Graphics2D g = (Graphics2D) g1d;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(BACKGROUND_COLOR);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        g.setFont(H5);
        drawAxes(g);
        drawLegend(g);
        drawData(g);
    }

    private record DataPoint(float surviveRate, float geneticDiversity) {
    }
}
