package com.nuuly;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the configuration class for Kafka settings.
 */
@Configuration
public class KafkaConfig<T> {

    /**
     * The bootstrap servers for the Kafka cluster.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * The group ID for the Kafka consumer.
     */
    @Value("${spring.kafka.consumer.group-id:inventory-group}")
    private String groupId;

    /**
     * This method creates a KafkaTemplate bean that can be used to send messages to Kafka topics.
     * @return A KafkaTemplate that can be used to send messages to Kafka topics
     */
    @Bean
    public KafkaTemplate<String, T> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * This method creates a DefaultKafkaProducerFactory bean that can be used to create Kafka producers for sending messages to Kafka topics.
     * @return A DefaultKafkaProducerFactory that can be used to create Kafka producers for sending messages to Kafka topics
     */
    @Bean
    public DefaultKafkaProducerFactory<String, T> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * This method creates a Map of Kafka producer configurations, including the bootstrap servers, key serializer, and value serializer.
     * @return A Map of Kafka producer configurations that can be used to create a Kafka producer for sending messages to Kafka topics
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return props;
    }

    /**
     * This method creates a ConsumerFactory bean that can be used to create Kafka consumers for receiving messages from Kafka topics.
     * @return A ConsumerFactory that can be used to create Kafka consumers for receiving messages from Kafka topics
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * This method creates a ConcurrentKafkaListenerContainerFactory bean that can be used to create Kafka listener containers for receiving messages from Kafka topics.
     * @return A ConcurrentKafkaListenerContainerFactory that can be used to create Kafka listener containers for receiving messages from Kafka topics
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
