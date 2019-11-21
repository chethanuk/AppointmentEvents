package com.chethanuk.utils;

import io.confluent.common.utils.Time;

import java.time.Instant;

/**
 * SystemTime implimentation to get SystemTime and sleep call
 */
public class SystemTime implements Time {

    /**
     * @param time Timestamp as string (Example: "2017-05-14T21:12:36Z")
     */
    public static long parseTimeStamp(final String time) {

        try {
            Instant instant = Instant.parse(time);

            return instant.toEpochMilli();

        } catch (final Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    /**
     * @see io.confluent.common.utils.Time#milliseconds()
     */
    @Override
    public long milliseconds() {
        return System.currentTimeMillis();
    }

    /**
     * @see io.confluent.common.utils.Time#nanoseconds()
     */
    @Override
    public long nanoseconds() {
        return System.nanoTime();
    }

    /**
     * @see io.confluent.common.utils.Time#sleep(long)
     */
    @Override
    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // no stress
        }
    }

}