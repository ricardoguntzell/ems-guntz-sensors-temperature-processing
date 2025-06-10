package br.com.guntz.sensors.temperature.processing.api.controller;

import br.com.guntz.sensors.temperature.processing.api.common.IdGenerator;
import br.com.guntz.sensors.temperature.processing.api.model.TemperatureLogOutput;
import io.hypersistence.tsid.TSID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static br.com.guntz.sensors.temperature.processing.infrastructure.rabbitmq.RabbitMQConfig.FANOUT_EXCHANGE_NAME;

@AllArgsConstructor
@RestController
@RequestMapping("/api/sensors/{sensorId}/temperatures/data")
@Slf4j
public class TemperatureProcessingController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public void data(@PathVariable TSID sensorId, @RequestBody String input) {
        if (input == null || input.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Double temperature;

        try {
            temperature = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        TemperatureLogOutput logOutput = TemperatureLogOutput.builder()
                .id(IdGenerator.generateTimeBasedUUID())
                .sensorId(sensorId)
                .value(temperature)
                .registeredAt(OffsetDateTime.now())
                .build();

        log.info(logOutput.toString());

        String exchange = FANOUT_EXCHANGE_NAME;
        String routingKey = "";
        Object payload = logOutput;

        MessagePostProcessor messagePostProcessor = message -> {
            message.getMessageProperties().setHeader("sensorId", logOutput.getSensorId().toString());
            return message;
        };

        rabbitTemplate.convertAndSend(exchange, routingKey, payload, messagePostProcessor);

    }
}
