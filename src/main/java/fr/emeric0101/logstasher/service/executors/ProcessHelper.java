package fr.emeric0101.logstasher.service.executors;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;

@Slf4j
public class ProcessHelper {

    interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load(Kernel32.class);
        int GetProcessId(Long hProcess);
    }
    /**
     * Force stop process
     * https://stackoverflow.com/questions/4912282/java-tool-method-to-force-kill-a-child-process
     * @param process
     */
    public static void stop(Process process) {

        try {
            int pid = getPid(process);
            if (pid != 0) {
                Process killProcess = Runtime.getRuntime().exec("taskkill /pid " + pid + " /f /T");
                killProcess.waitFor();
            } else {
                log.error("Unable to kill process");
            }


        } catch (Exception ex) {
            log.error("Unable to kill process");
        }

    }

    public static int getPid(Process p) {
        Field f;

        if (Platform.isWindows()) {
            try {
                f = p.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                int pid = Kernel32.INSTANCE.GetProcessId((Long) f.get(p));
                return pid;
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("Unable to get PID from process");
            }
        } else if (Platform.isLinux()) {
            try {
                f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                int pid = (Integer) f.get(p);
                return pid;
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("Unable to get PID from process");
            }
        }
        else{}
        return 0;
    }
}
