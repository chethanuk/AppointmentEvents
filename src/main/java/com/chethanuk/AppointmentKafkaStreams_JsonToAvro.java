package com.chethanuk;

import com.chethanuk.appointment.Appointment;
import com.chethanuk.appointment.Data;
import com.chethanuk.utils.SystemTime;
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
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;


public class AppointmentKafkaStreams_JsonToAvro {

    private static final String SOURCE_TOPIC = "appointment_jsons";
    private static final String SINK_TOPIC = "appointments_avro";

    private static final String SCHEMA_REGISTER_URL = "http://10.160.0.48:8081/";
    private static final String BOOTSTRAP_SERVERS = "10.160.0.48:9092";


    private static final Logger log = LoggerFactory.getLogger(AppointmentKafkaStreams_JsonToAvro.class);

    public static void main(final String[] args) {

        final KafkaStreams streams = buildAvroStream(
                BOOTSTRAP_SERVERS,
                SCHEMA_REGISTER_URL,
                SOURCE_TOPIC,
                SINK_TOPIC
        );

        //        streams.cleanUp(); // Not for production, just for this demo
        //        final CountDownLatch startLatch = new CountDownLatch(1);
        //        streams.setStateListener((newState, oldState) -> {
        //            if (newState == KafkaStreams.State.RUNNING && oldState != KafkaStreams.State.RUNNING) {
        //                startLatch.countDown();
        //            }


        // To catch any unexpected exceptions
        streams.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            log.error("Error: " + e.getMessage());
            // Depends on how to deal with Uncaught exceptions
            // streams.close();
        });


        streams.start();

        //        try {
        //            if (!startLatch.await(60, TimeUnit.SECONDS)) {
        //                throw new RuntimeException("Streams never finished rebalancing on startup");
        //            }
        //        } catch (final InterruptedException e) {
        //            Thread.currentThread().interrupt();
        //        }

        // Shutdown hook to respend for SIGTERM & Gracefully Shutdown Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    static KafkaStreams buildAvroStream(final String bootstrapServers,
                                        final String schemaRegistryUrl,
                                        final String source_topic,
                                        final String sink_topic) {


        final StreamsBuilder builder = new StreamsBuilder();

        final ObjectMapper objectMapper = new ObjectMapper();

        final Serde<String> stringSerde = Serdes.String();

        // Read Stream
        final KStream<String, String> jsonStream = builder.stream(source_topic,
                Consumed.with(stringSerde, stringSerde));

        jsonStream.mapValues(v -> {
            Appointment appointment = null;

            try {
                final JsonNode jsonNode = objectMapper.readTree(v);

                appointment = new Appointment(jsonNode.get("Type").asText(),
                        Data.newBuilder()
                                .setAppointmentId(jsonNode.get("Data").get("AppointmentId").asText())
                                .setTimestampUtc(SystemTime.parseTimeStamp(jsonNode.get("Data").get("TimestampUtc").asText()))
                                .build());
            } catch (IOException ie) {
                // Error
                // Push error and values(v) into kafka topic to analyse
                log.info("Exception: " + ie + "\n Value: " + v);
                // If you want to stop when there is bad data then throw Runtime exception else comment next line
                // throw new RuntimeException(e);
            } catch (Exception e) {
                // Error
                // Push error and values(v) into kafka topic to analyse
                log.info("Exception: " + e + "\n Value: " + v);
                // If you want to stop when there is bad data then throw Runtime exception else comment next line
                // throw new RuntimeException(e);
            }

            return appointment;

        }).filter((k, v) -> v != null).to(sink_topic);

        return new KafkaStreams(builder.build(), getProperties(bootstrapServers, schemaRegistryUrl, "/tmp/"));

    }


    private static Properties getProperties(final String bootStrapServers,
                                            final String schemaRegistry,
                                            final String stateDirectory) {


        final Properties props = new Properties();

        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "appointments-json-to-avro-stream-random-1");
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "appointments-json-to-avro-stream-random-1");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100 * 1000);

        // props.put(StreamsConfig.STATE_DIR_CONFIG, stateDirectory);

        return props;
    }
}