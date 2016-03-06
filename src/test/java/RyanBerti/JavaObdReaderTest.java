package RyanBerti;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileOutputStream;

/**
 * Created by Ryan Berti on 2/18/16.
 */
public class JavaObdReaderTest {

    private static int numCommands = 500;

    @Test
    @Category(RyanBerti.SerialTests.class)
    public void runJavaObdReaderSerialTest() {
        try {

            JavaObdReader jobdinstance = JavaObdReader.getJavaObdReaderForSerialPort(System.getProperty("test.serial.port"));

            jobdinstance.initOBDControlCommands();
            jobdinstance.initSupportedOdbCommands();
            System.out.println("Connection initialized successfully");

            FileOutputStream fos1 = new FileOutputStream("obdheader.txt");
            fos1.write(jobdinstance.getCommandListAsString().getBytes());
            fos1.close();

            long startTime = System.currentTimeMillis();
            FileOutputStream fos2 = new FileOutputStream("obddata.txt");
            for (int i = 0; i < numCommands; i++) {
                fos2.write(jobdinstance.runCommandsReturnString().getBytes());
                fos2.write('\n');
            }
            fos2.close();
            long endTime = System.currentTimeMillis();
            System.out.println("Running " + (numCommands * jobdinstance.getCommandCount()) + "commands  took " +
                                ((endTime - startTime) / 1000) + " seconds");

            jobdinstance.closeOBDConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Category(RyanBerti.SocketTests.class)
    public void runJavaObdReaderSocketTest() {
        try {

            JavaObdReader jobdinstance = JavaObdReader.getJavaObdReaderForSocket(System.getProperty("test.socket.host"),
                    Integer.valueOf(System.getProperty("test.socket.port")));

            jobdinstance.initOBDControlCommands();
            jobdinstance.initSupportedOdbCommands();
            System.out.println("Connection initialized successfully");

            FileOutputStream fos1 = new FileOutputStream("obdheader.txt");
            fos1.write(jobdinstance.getCommandListAsString().getBytes());
            fos1.close();

            long startTime = System.currentTimeMillis();
            FileOutputStream fos2 = new FileOutputStream("obddata.txt");
            for (int i = 0; i < numCommands; i++) {
                fos2.write(jobdinstance.runCommandsReturnString().getBytes());
                fos2.write('\n');
            }
            fos2.close();
            long endTime = System.currentTimeMillis();
            System.out.println("Running " + (numCommands * jobdinstance.getCommandCount()) + "commands  took " +
                    ((endTime - startTime) / 1000) + " seconds");

            jobdinstance.closeOBDConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
