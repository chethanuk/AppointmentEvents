package com.chethanuk.utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AppointmentUtils {

    public static String generateRandomAppointmentType() {
        List<String> givenList = Arrays.asList("AppointmentBooked", "AppointmentComplete", "AppointmentCancelled");
        Random rand = new Random();
        String randomElement = givenList.get(rand.nextInt(givenList.size()));

        return randomElement;
    }

    public static String generateAppointmentId() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }


    public static Long generateTimestampUTC() {

        Instant instant = Instant.now();
        // ZoneId zoneId = ZoneId.of( "GMT" );
        // ZonedDateTime zdt = instant.atZone( zoneId );

        return instant.toEpochMilli();
    }

    public static String generateDiscipline() {

        List<String> givenList = Arrays.asList("Physio", "Psychiatry", "Surgery", "Gynaecology");
        Random rand = new Random();
        String randomElement = givenList.get(rand.nextInt(givenList.size()));

        return randomElement;
    }

}
