package RyanBerti;

import com.github.pires.obd.commands.ObdMultiCommand;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.PercentageObdCommand;
import com.github.pires.obd.commands.pressure.PressureCommand;
import com.github.pires.obd.commands.protocol.*;
import com.github.pires.obd.commands.temperature.TemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import jssc.*;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by Ryan Berti on 2/18/16.
 */
public class JavaObdReader{

    //public for now so SimpleBenchmarkTests can validate throughput
    InputStream is;
    OutputStream os;

    private int numCommands = 0;
    private ObdMultiCommand multiCommand;
    private List<String> commandList;

    private JavaObdReader() {}

    /**
     * Static factory method for creating JavaObdReader instances that read commands from sockets
     * @param host The hostname or ip address of the OBD2 device (if connected to OBD2 device via
     *             wifi, this is usually 192.168.0.10)
     * @param port The port number of the OBD2 server (by default, wifi enabled OBD2 devices run
     *             their server on port 3500)
     * @return A JavaOBDReader instance
     * @throws IOException
     */
    public static JavaObdReader getJavaObdReaderForSocket(String host, Integer port) throws IOException {

        JavaObdReader reader = new JavaObdReader();

        Socket OBDsocket = new Socket();
        OBDsocket.connect(new InetSocketAddress(host,port),10);
        reader.is = OBDsocket.getInputStream();
        reader.os = OBDsocket.getOutputStream();
        return reader;
    }

    /**
     * Static factory method for creating JavaObdReader instances that read commands from serial ports
     * directly (this can also be used for interfacing with obdsim)
     * @param portName The serial port name (usually in the form /dev/ttys[0-9]{4})
     * @return A JavaOBDReader instance
     * @throws SerialPortException
     */
    public static JavaObdReader getJavaObdReaderForSerialPort(String portName) throws SerialPortException {

        JavaObdReader reader  = new JavaObdReader();

        SerialPort sp = new SerialPort(portName);
        sp.openPort();
        sp.setParams(SerialPort.BAUDRATE_9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        sp.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                SerialPort.FLOWCONTROL_RTSCTS_OUT);

        SerialInputStream sis = new SerialInputStream(sp);
        sis.setTimeout(255);
        reader.is = sis;
        reader.os = new SerialOutputStream(sp);
        return reader;
    }

    /**
     * Submit the OBD control commands which may or may not be necessary for the given OBD2 device
     * @throws IOException
     * @throws InterruptedException
     */
    public void initOBDControlCommands() throws IOException, InterruptedException {

        /**
         * Unclear if these are required when testing against obdsim, or even if they're
         * necessary when testing against hardware, though they are included in this post
         * http://blog.lemberg.co.uk/how-guide-obdii-reader-app-development
         */

        try {
            new EchoOffCommand().run(is, os);
        } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}

        try {
            new LineFeedOffCommand().run(is, os);
        } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}

        try {
            new TimeoutCommand(255).run(is, os); //these don't work with obdsim
        } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}

        try {
            new SelectProtocolCommand(ObdProtocols.AUTO).run(is, os);
        } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
    }

    /**
     * Determine all of the commands that are supported by the given OBD2 device by submitting each
     * OBDCommand instance and adding that command to the internal ObdMultiCommand instance if the
     * response is valid; set the ObdMultiCommand instance to read raw data only
     * @throws IOException
     * @throws InterruptedException
     */
    public void initSupportedOdbCommands() throws IOException, InterruptedException {
        initSupportedOdbCommands(Integer.MAX_VALUE);
    }

    /**
     * Get the first N commands that are supported by the given OBD2 device by submitting each
     * OBDCommand instance and adding that command to the internal ObdMultiCommand instance if the
     * response is valid; set the ObdMultiCommand instance to read raw data only
     * @throws IOException
     * @throws InterruptedException
     */
    public void initSupportedOdbCommands(int maxCommands) throws IOException, InterruptedException {

        numCommands = maxCommands;
        commandList = new ArrayList<>();
        multiCommand = new ObdMultiCommand();

        if (maxCommands > 0)
            maxCommands -= addSupportedEngineCommands(multiCommand, maxCommands);
        if (maxCommands > 0)
            maxCommands -= addSupportedFuelCommands(multiCommand, maxCommands);
        if (maxCommands > 0)
            maxCommands -= addSupportedPressureCommands(multiCommand,maxCommands);
        if (maxCommands > 0)
            maxCommands -= addSupportedTemperatureCommands(multiCommand,maxCommands);

        numCommands -= maxCommands;

        multiCommand.setConvertRawData(false);
    }

    /**
     * Run the OBDCommands that are associated with the internal ObdMultiCommand instance
     * @return A comma separated string of raw data
     * @throws IOException
     * @throws InterruptedException
     */
    public String runCommandsReturnRawData() throws IOException, InterruptedException {
        multiCommand.sendCommands(is, os);
        return multiCommand.getRawResult();
    }

    /**
     * Run the OBDCommands that are associated with the internal ObdMultiCommand instance
     * @return A comma separated string of formatted data
     * @throws IOException
     * @throws InterruptedException
     */
    public String runCommandsReturnFormattedResult() throws IOException, InterruptedException {
        multiCommand.sendCommands(is, os);
        return multiCommand.convertRawResultToFormattedResult(multiCommand.getRawResult());
    }

    /**
     * Loop through all of the engine commands defined by the java-obd-api library; submit each command
     * to determine if it is supported by the given OBD2 device and if so add it to the ObdMultiCommand
     * instance. Only add maxCommands to the ObdMultiCommand instance
     * @param multicmd the ObdMultiCommand instance
     * @param maxCommands the maximum number of engine commands to add to the ObdMultiCommand instance
     * @return the total number of engine commands added to the ObdMultiCommand instance
     * @throws IOException
     * @throws InterruptedException
     */
    private int addSupportedEngineCommands(ObdMultiCommand multicmd, int maxCommands) throws IOException, InterruptedException {

        if (multicmd == null)
            throw new IOException("Input arg must be non-null");

        if (maxCommands == 0)
            return 0;

        int commandsAdded = 0;

        try {

            Reflections reflections = new Reflections("com.github.pires.obd.commands.engine");
            Set<Class<? extends ObdCommand>> s1 = reflections.getSubTypesOf(ObdCommand.class);
            for (Class<? extends ObdCommand> c : s1) {
                ObdCommand obdcmd = c.newInstance();
                try {
                    obdcmd.run(is,os);
                    multicmd.add(obdcmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to engine multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

            Set<Class<? extends TemperatureCommand>> s2 = reflections.getSubTypesOf(TemperatureCommand.class);
            for (Class<? extends TemperatureCommand> c : s2) {
                TemperatureCommand tempcmd = c.newInstance();
                try {
                    tempcmd.run(is,os);
                    multicmd.add(tempcmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to engine multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

            Set<Class<? extends PercentageObdCommand>> s3 = reflections.getSubTypesOf(PercentageObdCommand.class);
            for (Class<? extends PercentageObdCommand> c : s3) {
                ObdCommand percentcmd = c.newInstance();
                try {
                    percentcmd.run(is,os);
                    multicmd.add(percentcmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to engine multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return commandsAdded;
    }

    /**
     * Loop through all of the fuel commands defined by the java-obd-api library; submit each command
     * to determine if it is supported by the given OBD2 device and if so add it to the ObdMultiCommand
     * instance. Only add maxCommands to the ObdMultiCommand instance
     * @param multicmd the ObdMultiCommand instance
     * @param maxCommands the maximum number of fuel commands to add to the ObdMultiCommand instance
     * @return the total number of fuel commands added to the ObdMultiCommand instance
     * @throws IOException
     * @throws InterruptedException
     */
    private int addSupportedFuelCommands(ObdMultiCommand multicmd, int maxCommands) throws IOException, InterruptedException {

        if (multicmd == null)
            throw new IOException("Input arg must be non-null");

        if (maxCommands == 0)
            return 0;

        int commandsAdded = 0;

        try {

            Reflections reflections = new Reflections("com.github.pires.obd.commands.fuel");
            Set<Class<? extends ObdCommand>> s1 = reflections.getSubTypesOf(ObdCommand.class);
            for (Class<? extends ObdCommand> c : s1) {
                ObdCommand obdcmd = c.newInstance();
                try {
                    obdcmd.run(is,os);
                    multicmd.add(obdcmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to fuel multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

            Set<Class<? extends TemperatureCommand>> s2 = reflections.getSubTypesOf(TemperatureCommand.class);
            for (Class<? extends TemperatureCommand> c : s2) {
                TemperatureCommand tempcmd = c.newInstance();
                try {
                    tempcmd.run(is,os);
                    multicmd.add(tempcmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to fuel multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

            Set<Class<? extends PercentageObdCommand>> s3 = reflections.getSubTypesOf(PercentageObdCommand.class);
            for (Class<? extends PercentageObdCommand> c : s3) {
                ObdCommand percentcmd = c.newInstance();
                try {
                    percentcmd.run(is,os);
                    multicmd.add(percentcmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to fuel multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return commandsAdded;

    }

    /**
     * Loop through all of the pressure commands defined by the java-obd-api library; submit each command
     * to determine if it is supported by the given OBD2 device and if so add it to the ObdMultiCommand
     * instance. Only add maxCommands to the ObdMultiCommand instance
     * @param multicmd the ObdMultiCommand instance
     * @param maxCommands the maximum number of fuel commands to add to the ObdMultiCommand instance
     * @return the total number of engine commands added to the ObdMultiCommand instance
     * @throws IOException
     * @throws InterruptedException
     */
    private int addSupportedPressureCommands(ObdMultiCommand multicmd, int maxCommands) throws IOException, InterruptedException {

        if (multicmd == null)
            throw new IOException("Input arg must be non-null");

        if (maxCommands == 0)
            return 0;

        int commandsAdded = 0;

        try {

            Reflections reflections = new Reflections("com.github.pires.obd.commands.pressure");
            Set<Class<? extends PressureCommand>> s1 = reflections.getSubTypesOf(PressureCommand.class);
            for (Class<? extends PressureCommand> c : s1) {
                PressureCommand pressurecmd = c.newInstance();
                try {
                    pressurecmd.run(is,os);
                    multicmd.add(pressurecmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to pressure multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return commandsAdded;
    }

    /**
     * Loop through all of the temperature commands defined by the java-obd-api library; submit each command
     * to determine if it is supported by the given OBD2 device and if so add it to the ObdMultiCommand
     * instance. Only add maxCommands to the ObdMultiCommand instance
     * @param multicmd the ObdMultiCommand instance
     * @param maxCommands the maximum number of temperature commands to add to the ObdMultiCommand instance
     * @return the total number of temperature commands added to the ObdMultiCommand instance
     * @throws IOException
     * @throws InterruptedException
     */
    private int addSupportedTemperatureCommands(ObdMultiCommand multicmd, int maxCommands) throws IOException, InterruptedException {

        if (multicmd == null)
            throw new IOException("Input arg must be non-null");

        if (maxCommands == 0)
            return 0;

        int commandsAdded = 0;

        try {

            Reflections reflections = new Reflections("com.github.pires.obd.commands.temperature");
            Set<Class<? extends TemperatureCommand>> s1 = reflections.getSubTypesOf(TemperatureCommand.class);
            for (Class<? extends TemperatureCommand> c : s1) {
                TemperatureCommand temperaturecmd = c.newInstance();
                try {
                    temperaturecmd.run(is,os);
                    multicmd.add(temperaturecmd);
                    commandList.add(c.toString());
                    System.out.println(c.toString() + " is added to pressure multicmd");
                    if (++commandsAdded == maxCommands)
                        return commandsAdded;
                } catch (UnsupportedCommandException | MisunderstoodCommandException | NoDataException e) {}
            }

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return commandsAdded;

    }

    /**
     * Get the number of ObdCommands associated with the internal ObdMultiCommand
     * @return number of ObdCommands
     */
    public int getCommandCount() {
        return numCommands;
    }

    /**
     * Get the full names of all the ObdCommands associated with the internal ObdMultiCommand
     * @return comma separated list of fully formed class names, bound by brackets
     */
    public String getCommandListAsString() {
        return Arrays.toString(commandList.toArray());
    }

    /**
     * Close the underlying input and output streams associated with this JavaObdReader instance
     * @throws IOException
     */
    public void closeOBDConnection() throws IOException {
        is.close();
        os.close();
    }
}
