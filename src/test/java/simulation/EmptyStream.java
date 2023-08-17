package simulation;

import java.io.OutputStream;
import java.io.PrintStream;

public class EmptyStream extends PrintStream {
    public EmptyStream() {
        super(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
    }
}
