package com.h2ph.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

public class Log4jFilter extends AbstractFilter {

    @Override
    public Result filter(LogEvent event) {
        if (event == null)
            return Result.NEUTRAL;

        // check message
        Message msg = event.getMessage();
        if (msg != null) {
            String text = msg.getFormattedMessage();
            if (shouldFilter(text))
                return Result.DENY;
        }

        // check throwable
        Throwable t = event.getThrown();
        while (t != null) {
            if (shouldFilter(t.getMessage()))
                return Result.DENY;
            t = t.getCause();
        }

        return Result.NEUTRAL;
    }

    private boolean shouldFilter(String msg) {
        if (msg == null)
            return false;
        return msg.contains("Status: 429") ||
                msg.contains("Couldn't look up profile properties") ||
                msg.contains("MinecraftClientHttpException") ||
                msg.contains("Could not connect to database!") ||
                msg.contains("Attempted reconnect");
    }
}
