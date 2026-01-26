package com.team8.damo.repository;

import com.team8.damo.entity.Restaurant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class RestaurantMongoRepositoryTest {

    @Autowired
    private RestaurantMongoRepository restaurantMongoRepository;

    @Test
    @DisplayName("식별자로 식당을 조회할 수 있다.")
    void findAll() {
//        Optional<RestaurantMongo> restaurant = restaurantMongoRepository.findById("6976b57f10e1fa815903d4cf");
//        System.out.println(restaurant);
//        assertThat(restaurant).isPresent();

        List<Restaurant> restaurants = restaurantMongoRepository.findByCategoryGroupName("음식점");
        restaurants.forEach(System.out::println);
        assertThat(restaurants).isNotEmpty();
    }
}
