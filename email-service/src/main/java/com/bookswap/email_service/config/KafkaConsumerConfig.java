package com.bookswap.email_service.config;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
public class KafkaConsumerConfig {

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      KafkaProperties kafkaProperties) {
    Map<String, Object> kafkaConsumerConfig = kafkaProperties.buildConsumerProperties(null);
    kafkaConsumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    kafkaConsumerConfig.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    kafkaConsumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    ConsumerFactory<String, String> consumerFactory =
        new DefaultKafkaConsumerFactory<>(kafkaConsumerConfig);

    ConcurrentKafkaListenerContainerFactory<String, String> kafkaConsumerFactory =
        new ConcurrentKafkaListenerContainerFactory<>();
    kafkaConsumerFactory.setConsumerFactory(consumerFactory);

    return kafkaConsumerFactory;
  }
}
