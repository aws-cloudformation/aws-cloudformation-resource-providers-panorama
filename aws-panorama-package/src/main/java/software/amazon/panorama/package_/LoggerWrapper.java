package software.amazon.panorama.package_;

import software.amazon.cloudformation.proxy.Logger;

public class LoggerWrapper {
    private Logger logger;

    public LoggerWrapper(final Logger logger) {
        this.logger = logger;
    }

    public void info(final String message) {
        this.log("INFO", message);
    }

    public void error(final String message) {
        this.log("ERROR", message);
    }

    private void log(String prefix, String message) {
        logger.log(prefix + " " + message + "\n");
    }
}
