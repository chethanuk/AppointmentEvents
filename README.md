# Appointment Events

Data will be inserted into Druid like following:

![Data in Druid](https://i.imgur.com/Xf2V7J4.png)


Current State of Appointment using KSQL(KTable):

```SQL
CREATE TABLE appointment_state (
`TYPE` VARCHAR,
APPOINTMENTID VARCHAR
) WITH (KEY='APPOINTMENTID', KAFKA_TOPIC = 'appointment_source', VALUE_FORMAT = 'AVRO');
```

Current State of Appointment using Apache Druid:

```SQL
SELECT Type FROM appointment_events
WHERE "AppointmentId" = '3cb0f939-9398-4d29-a28f-2a1a3  a6ce3b2'
ORDER BY "__time" DESC LIMIT 1;
```

![Output](https://i.imgur.com/ysa0hFe.png)

Total Time Spent by Customer per Appointment:
```SQL
SELECT AppointmentId AS APPOINTMENT_Id,
  (CASE WHEN COUNT(*) > 1 
        THEN (MAX(TimestampUtc) - MIN(TimestampUtc))
        ELSE 0
        END) AS AVG_TIMESPENT
FROM appointments
GROUP BY AppointmentId
```
Average Time between 'AppointmentBooked' and 'AppointmentComplete':
```SQL
SELECT AVG(TIMESPENT) AS AVG_TIMESPENT
FROM (
SELECT AppointmentId AS APPOINTMENT_ID,
  (CASE WHEN COUNT(*) > 1 
        THEN (MAX(TimestampUtc) - MIN(TimestampUtc))
        ELSE 0
        END) AS TIMESPENT
FROM appointments
WHERE Type = 'AppointmentBooked' OR Type = 'AppointmentComplete'
GROUP BY AppointmentId
) 
```


## Files in this Repo

1. kubernetes-manifests
    - Basic manifests for deployment this Java application into Google Kubernetes Engine(GKE)
2. src/main/com.chethanuk/utils:
    - AppointmentUtils: Utility class for generating random appointment data points
    - KafkaUtils: Kafka Producer Utility class
    - SystemTime: SystemTime implimentation to get SystemTime and sleep call
3. src/main/com.chethanuk/
    - **AppointmentKafkaStreams_JsonToAvro.java**: 
        When others dont want to move into Apache Avro or Protobuf(.proto) data format, we have to write application to convert the json schema events into Avro data points
        
        This class is a Simple implementation of Kafka Streams, which consume data from Kafka topic with json (or String Serde) and clean & parse and then produce data in Avro format
    - **AppointmentAvroProducer.java**: 
        Kafka Producer, which generate random Appointment events with Schema version v0 to Kafka topic in avro format 
    - **AppointmentAvroProducerV1.java**:
        Kafka Producer, which generate random Appointment events with Schema version v1 to Kafka topic in avro format
        
    - **AppointmentConsumer.java**:
        Simple Kafka Consumer, which reads data from Kafka Topic (Avro data format)
    - **AppointmentCurrentState_KafkaStreams.java**:
        In an optimal situation, all the Kafka producers in an organization will produce and consume data in Apache Avro or Protobuf data format..
        
        This is a Kafka Streams application, which reads data from SOURCE_TOPIC = "appointments_avro", filter, transform and select the key for kafka message so other application(like KSQL or KStreams or KTable) can use this and then produce the messages in Avro data format
4. src/resources/avro:
    - *.avsc: Avro Schemas (AVSC)
5. Dockerfile: To build docker image for this Java application
6. druid_injestion_spec:
    - To injest the Kafka messages into Apache Druid
7. skaffold.yaml: 
    - GKE utility to build, push and deploy the java application into Stagging or Dev GKE

Schema Evolution:

Before evolving the schema, make sure the test will pass, 

- Within the application, disable automatic schema registration by setting the configuration parameter auto.register.schemas=false.
  
```java
props.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
```

- Change the following and point it to schema you want to evolve
```xml
<transactions-value>
    src/main/resources/avro/com/chethanuk/appointment/appointment-v1.avsc
</transactions-value>
```

If there is proper CI/CD built in already, typicall Docker build will kickoff and then tests will show whether schema is compatible or not

If not, Run the compatibility check and verify that it fails:
```bash
mvn io.confluent:kafka-schema-registry-maven-plugin:5.0.0:test-compatibility        
```

It will result in:

```bash
[INFO] Schema ... is compatible with subject
```

OR  
```bash
[ERROR] Schema ... is not compatible with subject
```


And we can also write compatibility tests:

### Compatibility Test:

```java
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AvroCompatibilityAppointmentsTest {
    
  private final String appointmentv0 = "{\n" +
                      "    \"name\": \"Appointment\",\n" +
                      "    \"type\": \"record\",\n" +
                      "    \"namespace\": \"com.chethanuk.appointment\",\n" +
                      "    \"fields\": [\n" +
                      "        {\n" +
                      "            \"name\": \"Type\",\n" +
                      "            \"type\": \"string\",\n" +
                      "            \"doc\": \"Current state of an appointment. Ex: AppointmentBooked\"\n" +
                      "        },\n" +
                      "        {\n" +
                      "            \"name\": \"Data\",\n" +
                      "            \"type\": {\n" +
                      "                \"name\": \"Data\",\n" +
                      "                \"type\": \"record\",\n" +
                      "                \"fields\": [\n" +
                      "                    {\n" +
                      "                        \"name\": \"AppointmentId\",\n" +
                      "                        \"type\": \"string\",\n" +
                      "                        \"doc\":\"Unique Id associated with each Appointment\"\n" +
                      "                    },\n" +
                      "                    {\n" +
                      "                        \"name\": \"TimestampUtc\",\n" +
                      "                        \"type\" : \"long\",\n" +
                      "                        \"default\": -1,\n" +
                      "                        \"doc\" : \"Unix epoch Time in seconds\"\n" +
                      "                    }\n" +
                      "                ]\n" +
                      "            },\n" +
                      "            \"doc\": \"contain core Data fields of Appointment\"\n" +
                      "        }\n" +
                      "    ]\n" +
                      "}";
  
  private final Schema appointment_schema_v0 = AvroUtils.parseSchema(appointmentv0).schemaObj;
  
  private final String appointmentv1 = "{\n" +
                      "    \"name\": \"Appointment\",\n" +
                      "    \"type\": \"record\",\n" +
                      "    \"namespace\": \"com.chethanuk.appointment\",\n" +
                      "    \"fields\": [\n" +
                      "        {\n" +
                      "            \"name\": \"Type\",\n" +
                      "            \"type\": \"string\",\n" +
                      "            \"doc\": \"Current state of an appointment. Ex: AppointmentBooked\"\n" +
                      "        },\n" +
                      "        {\n" +
                      "            \"name\": \"Data\",\n" +
                      "            \"type\": {\n" +
                      "                \"name\": \"Data\",\n" +
                      "                \"type\": \"record\",\n" +
                      "                \"fields\": [\n" +
                      "                    {\n" +
                      "                        \"name\": \"AppointmentId\",\n" +
                      "                        \"type\": \"string\",\n" +
                      "                        \"doc\":\"Unique Id associated with each Appointment\"\n" +
                      "                    },\n" +
                      "                    {\n" +
                      "                        \"name\": \"TimestampUtc\",\n" +
                      "                        \"type\" : \"long\",\n" +
                      "                        \"logicalType\" : \"timestamp-millis\",\n" +
                      "                        \"default\": -1,\n" +
                      "                        \"doc\": \"Unix epoch Time in seconds\"\n" +
                      "                    },\n" +
                      "                    {\n" +
                      "                        \"name\": \"Discipline\",\n" +
                      "                        \"type\": {\n" +
                      "                            \"type\": \"array\",\n" +
                      "                            \"items\": \"string\"\n" +
                      "                        },\n" +
                      "                        \"default\": [],\n" +
                      "                        \"doc\": \"Discipline of Appointment. Example: Physio\"\n" +
                      "                   }\n" +
                      "                ]\n" +
                      "            },\n" +
                      "            \"doc\": \"contain core Data fields of Appointment\"\n" +
                      "        }\n" +
                      "    ]\n" +
                      "}";
  
  private final Schema appointment_schema_v1 = AvroUtils.parseSchema(appointmentv1).schemaObj;
  
  /*
     * Compatibility Test
     */
    @Test
    public void testBasicBackwardsCompatibility() {
      AvroCompatibilityChecker checker = AvroCompatibilityChecker.BACKWARD_CHECKER;
      assertTrue("adding a field with default is a backward compatible change",
                 checker.isCompatible(schema2, Collections.singletonList(schema1)));
    }
    
    // TO DO: Depending on what our schema should be, we can write following tests 
    //  1. Forward compatibility Test
    //  2. Forward transitive compatibility Tests
    //  3. Backward compatibility
    //  4. Backward transitive compatibility
    //  5. Full compatibility
  
}
```