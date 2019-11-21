package com.chethanuk;

import com.chethanuk.appointment.Appointment;
import com.chethanuk.appointment.AppointmentFlattened;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class AppointmentCurrentState_KafkaStreams {

    private static final String SOURCE_TOPIC = "appointments_avro";
    private static final String SINK_TOPIC = "appointments_stream";

    private static final String TABLE_SINK_TOPIC = "appointments_ktable";

    private static final String SCHEMA_REGISTER_URL = "http://10.160.0.48:8081/";
    private static final String BOOTSTRAP_SERVERS = "10.160.0.48:9092";

    private static final Serde<Long> longSerde = Serdes.Long();

    private static final Logger log = LoggerFactory.getLogger(AppointmentCurrentState_KafkaStreams.class);

    public static void main(final String[] args) {

        final KafkaStreams streams = buildCurrentStateStream(
                BOOTSTRAP_SERVERS,
                SCHEMA_REGISTER_URL,
                SOURCE_TOPIC,
                SINK_TOPIC
        );

        // To catch any unexpected exceptions
        streams.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            log.error("Error: " + e.getMessage());
            // Depends on how to deal with Uncaught exceptions
            // streams.close();

            // Or uncomment following line
            // throw new RuntimeException(e);
        });

        streams.start();

        // Shutdown hook to respend for SIGTERM & Gracefully Shutdown Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static KafkaStreams buildCurrentStateStream(final String bootstrapServers,
                                                        final String schemaRegistryUrl,
                                                        final String source_topic,
                                                        final String sink_topic) {


        final Map<String, String> serdeConfig =
                Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        schemaRegistryUrl);


        final SpecificAvroSerde<Appointment> appointmentSerde = new SpecificAvroSerde<>();
        appointmentSerde.configure(serdeConfig, false);

        final StreamsBuilder builder = new StreamsBuilder();

        // Read Stream
        final KStream<Long, Appointment> appointmentKStream = builder.stream(source_topic,
                Consumed.with(longSerde, appointmentSerde));

        appointmentKStream
                .filter((k, v) -> v.getData().getAppointmentId() != null)
                .mapValues(v -> {
                    AppointmentFlattened appointmentFlattened = null;

                    try {

                        appointmentFlattened = AppointmentFlattened.newBuilder().setAppointmentId(v.getData().getAppointmentId())
                                .setType(v.getType())
                                .setTimestampUtc(v.getData().getTimestampUtc().longValue()).build();

                        return appointmentFlattened;

                    } catch (final Exception e) {
                        // Error
                        // Push error and values(v) into kafka topic to analyse
                        // Push error and values(v) into kafka topic to analyse
                        log.info("Exception: " + e + "\n Value: " + v);
                        // If you want to stop when there is bad data then throw Runtime exeption else comment next line
                        // throw new RuntimeException(e);
                    }

                    return appointmentFlattened;
                })
                .selectKey((key, value) -> value.getAppointmentId().toString().toLowerCase())
                .to(sink_topic);

//        KTable<String, AppointmentFlattened> appointmentFlattenedKTable = builder.table(sink_topic);
//
//        appointmentFlattenedKTable
//                .filter((k, v) -> v.getAppointmentId() != null)
//                .toStream()
//                .to(TABLE_SINK_TOPIC);

        return new KafkaStreams(builder.build(), getProperties(BOOTSTRAP_SERVERS, SCHEMA_REGISTER_URL, "/tmp"));

    }


    private static Properties getProperties(final String bootStrapServers,
                                            final String schemaRegistry,
                                            final String stateDirectory) {


        final Properties props = new Properties();

        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "Appointment-current-state");
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "Appointment-current-state-client");

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "appointment_state_group_id");
        // props.put(ConsumerConfig.CLIENT_ID_CONFIG, "appointment_state_client_id");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");

        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "2");
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "15000");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);

        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100 * 1000);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);

        // props.put(StreamsConfig.STATE_DIR_CONFIG, stateDirectory);

        return props;
    }
}