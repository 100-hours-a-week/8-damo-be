package com.team8.damo.fixture;

import com.team8.damo.entity.Restaurant;
import org.springframework.test.util.ReflectionTestUtils;

public class RestaurantFixture {

    public static Restaurant create(String id) {
        Restaurant restaurant = new Restaurant();
        ReflectionTestUtils.setField(restaurant, "id", id);
        ReflectionTestUtils.setField(restaurant, "placeName", "맛있는 식당");
        ReflectionTestUtils.setField(restaurant, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(restaurant, "categoryGroupName", "음식점");
        ReflectionTestUtils.setField(restaurant, "address", "서울시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(restaurant, "latitude", "37.5012");
        ReflectionTestUtils.setField(restaurant, "longitude", "127.0396");
        ReflectionTestUtils.setField(restaurant, "isReservable", true);
        ReflectionTestUtils.setField(restaurant, "naverUrl", "https://naver.com/restaurant");
        return restaurant;
    }

    public static Restaurant create(String id, String placeName) {
        Restaurant restaurant = new Restaurant();
        ReflectionTestUtils.setField(restaurant, "id", id);
        ReflectionTestUtils.setField(restaurant, "placeName", placeName);
        ReflectionTestUtils.setField(restaurant, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(restaurant, "categoryGroupName", "음식점");
        ReflectionTestUtils.setField(restaurant, "address", "서울시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(restaurant, "latitude", "37.5012");
        ReflectionTestUtils.setField(restaurant, "longitude", "127.0396");
        ReflectionTestUtils.setField(restaurant, "isReservable", true);
        ReflectionTestUtils.setField(restaurant, "naverUrl", "https://naver.com/restaurant");
        return restaurant;
    }
}
