package com.chethanuk;

import com.chethanuk.appointment.Appointment;
import com.chethanuk.appointment.Data;
import com.chethanuk.utils.AppointmentUtils;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class AppointmentAvroProducerV1 {

    private static final String BOOTSTRAP_SERVERS = "10.160.0.48:9092";
    private static final String TOPIC = "tests1";
    private static final String SCHEMA_REGISTER_URL = "http://10.160.0.48:8081/";

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(final String[] args) {

        // V0: { "Type": "AppointmentComplete", "Data": { "AppointmentId": "757001d9-a454-40d3-a14b-9f0f9440be9f", "": "1380480379000"} }
        // V1: { "Type": "AppointmentBooked", "Data": { "AppointmentId": "8825cdff-f172-4132-9793-864b4dd72444", "TimestampUtc": "2017-05-14T22:12:37Z", "Discipline": ["Physio"] } }
        try (KafkaProducer<String, Appointment> producer = new KafkaProducer<String, Appointment>(getProperties())) {

            int totalNumMessage = 10000;

            for (long i = 0; i < totalNumMessage; i++) {

                final String appointmentType = AppointmentUtils.generateRandomAppointmentType();

                final String appointmentId = AppointmentUtils.generateAppointmentId();

                final long timeStampUTC = AppointmentUtils.generateTimestampUTC();

                final String discipline = AppointmentUtils.generateDiscipline();

                final Appointment appointment = Appointment.newBuilder()
                        .setType(appointmentType)
                        .setData(Data.newBuilder().setAppointmentId(appointmentId)
                                .setTimestampUtc(timeStampUTC)
                                // @TODO: Change the avsc in pom.xml and uncomment next line to produce v1 data
                                // .setDiscipline(discipline)
                                .build())
                        .build();

                final ProducerRecord<String, Appointment> record = new ProducerRecord<String, Appointment>(TOPIC, "1", appointment);
                producer.send(record);
                Thread.sleep(100L);
            }


            producer.flush();
            System.out.printf("Successfully produced %s messages to a topic called %s%n", totalNumMessage, TOPIC);

        } catch (final SerializationException e) {
            e.printStackTrace();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

    }


    private static Properties getProperties() {

        final Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTER_URL);
        props.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, true);

        return props;
    }
}

