package RyanBerti;

import jssc.SerialPortException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

/**
 * Created by Ryan Berti on 3/2/16.
 */
public class SimpleBenchmarkTest {

    private static int numCommands = 5000;

    @Test
    @Category(RyanBerti.SerialTests.class)
    public void runSimpleSerialCommandsTest() {
        try {

            JavaObdReader reader = JavaObdReader.getJavaObdReaderForSerialPort(System.getProperty("test.serial.port"));

            long startTime = System.currentTimeMillis();
            for (int i = 0; i<numCommands; i++) {
                reader.os.write("01 0C\r".getBytes());
                reader.os.flush();
                byte b;
                char c;
                    while (((b = (byte) reader.is.read()) > -1)) {
                        c = (char) b;
                        if (c == '>') // read until '>' arrives
                        {
                            break;
                        }
                    }
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Running " + numCommands + " commands took " + ((endTime - startTime) / 1000) + " seconds");
            reader.closeOBDConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    @Category(RyanBerti.SocketTests.class)
    public void runSimpleSocketCommandsTest() {
        try {

            JavaObdReader reader = JavaObdReader.getJavaObdReaderForSocket(System.getProperty("test.socket.host"),
                    Integer.valueOf(System.getProperty("test.socket.port")));

            long startTime = System.currentTimeMillis();
            for (int i = 0; i<numCommands; i++) {
                reader.os.write("01 0C\r".getBytes());
                reader.os.flush();
                byte b;
                char c;
                while (((b = (byte) reader.is.read()) > -1)) {
                    c = (char) b;
                    if (c == '>') // read until '>' arrives
                    {
                        break;
                    }
                }
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Running " + numCommands + " commands took " + ((endTime - startTime) / 1000) + " seconds");
            reader.closeOBDConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
