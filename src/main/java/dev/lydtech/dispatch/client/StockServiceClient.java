package dev.lydtech.dispatch.client;

import dev.lydtech.dispatch.exception.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class StockServiceClient {

    private final RestTemplate restTemplate;

    private final String stockServiceEndPoint;


    public StockServiceClient(@Autowired RestTemplate restTemplate, @Value("${dispatch.stockServiceEndpoint}") String stockServiceEndPoint) {
        this.restTemplate = restTemplate;
        this.stockServiceEndPoint = stockServiceEndPoint;
    }

    public String checkAvailability(String item) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(stockServiceEndPoint + "?item=" + item, String.class);
            if (response.getStatusCodeValue() != 200) {
                throw new RuntimeException("Error " + response.getStatusCodeValue());
            }
            return response.getBody();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("Error checking availability", e);
            throw new RetryableException(e);
        } catch (Exception e) {
            log.error("Exception thrown: " + e.getClass().getName(), e);
            throw e;
        }
    }
}
