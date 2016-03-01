package RyanBerti;

import org.junit.Test;

/**
 * Created by Ryan Berti on 2/18/16.
 */
public class JavaObdReaderTest {

    private static int numCommands = 100;

    @Test
    public void runCommandsTest() {
        try {

            /**
             * When running this test, obdsim needs to be running and the serial port string needs to
             * be updated accordingly
             */

            //JavaObdReader jobdinstance = JavaObdReader.getJavaObdReaderForSerialPort("/dev/ttys002");
            JavaObdReader jobdinstance = new JavaObdReader("/dev/ttys002");

            jobdinstance.initOBDControlCommands();
            jobdinstance.initSupportedOdbCommands();
            System.out.println("Connection initialized successfully");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < numCommands; i++) {
                System.out.println(jobdinstance.runCommandsReturnString());
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Running " + numCommands + " took " + ((endTime - startTime) / 1000) + " seconds");

            jobdinstance.closeOBDConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
