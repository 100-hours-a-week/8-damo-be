package com.team8.damo.repository;

import com.team8.damo.entity.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    List<Restaurant> findByCategoryGroupName(String categoryGroupName);
}
