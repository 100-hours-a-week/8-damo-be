package com.team8.damo.repository;

import com.team8.damo.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class RestaurantRepositoryTest {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Test
    @DisplayName("식별자로 식당을 조회할 수 있다.")
    void findAll() {
        List<Restaurant> restaurants = restaurantRepository.findByCategoryGroupName("음식점");
        assertThat(restaurants).isNotEmpty();
    }
}
