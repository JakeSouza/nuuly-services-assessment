package com.nuuly;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
* This is the configuration class for the ProducerService. It autowires the KafkaProducerService and provides a bean for the ProducerService that can be used in other parts of the application.
*/
@Configuration
public class ProducerConfig {

    /**
     * This is the configuration class for the ProducerService. It autowires the KafkaProducerService and provides a bean for the ProducerService that can be used in other parts of the application.
    */
    @Autowired
    private KafkaProducerService<String> kafkaProducerService;
    
    /**
     * This method creates a bean for the ProducerService that can be autowired into other classes. It uses the autowired KafkaProducerService to create an instance of the ProducerService.
     * @return A new instance of ProducerService that can be used to send messages to Kafka topics
    */
    @Bean
    public ProducerService producer() {
        return new ProducerService(kafkaProducerService);
    }
}
