package com.team8.damo.repository;

import com.team8.damo.entity.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantMongoRepository extends MongoRepository<Restaurant, String> {

    List<Restaurant> findByCategoryGroupName(String categoryGroupName);
}
