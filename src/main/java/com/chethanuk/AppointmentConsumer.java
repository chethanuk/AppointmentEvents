package com.chethanuk;

import com.chethanuk.appointment.Appointment;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;
import java.util.Properties;

public class AppointmentConsumer {

    private static final String TOPIC = "appointment_avro_source";
    private static final String SCHEMA_REGISTER_URL = "http://localhost:8081/";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final Integer SCHEMA_VERSION = 1;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(final String[] args) {


        try (final KafkaConsumer<String, Appointment> consumer = new KafkaConsumer<>(getProperties())) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            while (true) {
                final ConsumerRecords<String, Appointment> records = consumer.poll(100);

                for (final ConsumerRecord<String, Appointment> appointmentConsumerRecord : records) {
                    final String key = appointmentConsumerRecord.key();
                    final Appointment value = appointmentConsumerRecord.value();
                    System.out.printf("Appointment: SchemaVersion = %s, Key = %s, value = %s%n", SCHEMA_VERSION, key, value);
                }
            }

        }
    }

    private static Properties getProperties() {

        final Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTER_URL);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, TOPIC + "_consumer_v0");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        return props;
    }
}
