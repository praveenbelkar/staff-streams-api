package education.nsw.icc.streams.util;

import education.nsw.streams.avro.api.StaffSink;
import education.nsw.streams.avro.api.StaffSource;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.kafka.client.serialization.JsonbSerde;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serde;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * See
 * https://github.com/confluentinc/kafka-streams-examples/blob/5.4.1-post/src/main/java/io/confluent/examples/streams/microservices/domain/Schemas.java
 */
@ApplicationScoped
public class Schemas {
  public static final JsonbSerde<DefaultId> DEFAULT_ID_SERDE = new JsonbSerde<>(DefaultId.class);

  @ConfigProperty(name = "quarkus.kafka-streams.schema-registry-url")
  String schemaRegistryUrl;

  @ConfigProperty(name = "staff.input.topic")
  String inputTopic;

  @ConfigProperty(name = "staff.output.topic")
  String outputTopic;

  @ConfigProperty(name = "kafka.user")
  Optional<String> kafkaUser;

  @ConfigProperty(name = "kafka.password")
  Optional<String> kafkaPassword;

  @ConfigProperty(name = "SCHEMA_REGISTRY_SSL_TRUSTSTORE_LOCATION")
  Optional<String> trustStore;

  @ConfigProperty(name = "SCHEMA_REGISTRY_SSL_TRUSTSTORE_PASSWORD")
  Optional<String> trustStorePassword;

  public void configureSerdes() {
    Topics.createTopics(inputTopic, outputTopic);
    for (final Topic<?, ?> topic : Topics.ALL.values()) {
      configure(topic.keySerde());
      configure(topic.valueSerde());
    }
  }

  private void configure(final Serde<?> serde) {
    if (serde instanceof SpecificAvroSerde) {
      Map<String, String> configMap = new HashMap<>();
      configMap.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
      if (kafkaUser.isPresent() && kafkaPassword.isPresent()) {
        configMap.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        configMap.put(
            SchemaRegistryClientConfig.USER_INFO_CONFIG,
            kafkaUser.get() + ":" + kafkaPassword.get());
      }
      if (trustStore.isPresent()) {
        configMap.put(
            SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            trustStore.get());
        if (trustStorePassword.isPresent()) {
          configMap.put(
              SchemaRegistryClientConfig.CLIENT_NAMESPACE
                  + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
              trustStorePassword.get());
        }
      }
      serde.configure(configMap, false);
    }
  }

  public static class Topic<K, V> {
    private final String name;
    private final Serde<K> keySerde;
    private final Serde<V> valueSerde;

    Topic(final String name, final Serde<K> keySerde, final Serde<V> valueSerde) {
      this.name = name;
      this.keySerde = keySerde;
      this.valueSerde = valueSerde;
      Topics.ALL.put(name, this);
    }

    public Serde<K> keySerde() {
      return keySerde;
    }

    public Serde<V> valueSerde() {
      return valueSerde;
    }

    public String name() {
      return name;
    }

    public String toString() {
      return name;
    }
  }

  public static class Topics {
    public static final Map<String, Topic<?, ?>> ALL = new HashMap<>();
    public static Topic<DefaultId, StaffSource> STAFF_SOURCE_TOPIC;
    public static Topic<DefaultId, StaffSink> STAFF_FINAL_TOPIC;

    private static void createTopics(String inputTopic, String outputTopic) {
      STAFF_SOURCE_TOPIC = new Topic<>(inputTopic, DEFAULT_ID_SERDE, new SpecificAvroSerde<>());
      STAFF_FINAL_TOPIC = new Topic<>(outputTopic, DEFAULT_ID_SERDE, new SpecificAvroSerde<>());
    }
  }
}
