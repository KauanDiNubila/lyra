package app.lyra.food.adapter.out.openfoodfacts;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "lyra.open-food-facts")
record OpenFoodFactsProperties(String baseUrl, String userAgent, Duration connectTimeout, Duration readTimeout) {
}
