package app.lyra.food.adapter.out.openfoodfacts;

import app.lyra.food.application.port.out.ProductCatalog;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.shared.domain.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.util.Optional;

@Slf4j
@Component
@EnableConfigurationProperties(OpenFoodFactsProperties.class)
class OpenFoodFactsCatalog implements ProductCatalog {

    private static final String PRODUCT_PATH = "/api/v2/product/{code}.json?fields=code,product_name,brands,product_quantity_unit,nutriments";

    private final RestClient client;

    OpenFoodFactsCatalog(OpenFoodFactsProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<Food> findByBarcode(Barcode barcode) {
        JsonNode body = fetch(barcode);
        return body == null ? Optional.empty() : OpenFoodFactsProductMapper.toFood(barcode, body);
    }

    private JsonNode fetch(Barcode barcode) {
        try {
            return client.get().uri(PRODUCT_PATH, barcode.value()).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException response) {
            if (response.getStatusCode().value() == 404) {
                return null;
            }
            throw unavailable(barcode, response);
        } catch (RestClientException failure) {
            throw unavailable(barcode, failure);
        }
    }

    private static ServiceUnavailableException unavailable(Barcode barcode, Exception cause) {
        log.warn("The Open Food Facts failed for the barcode {}", barcode, cause);
        return new ServiceUnavailableException("The product source is unavailable, try again later", cause);
    }
}
