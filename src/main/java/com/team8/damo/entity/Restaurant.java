package com.team8.damo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Document(collection = "restaurants")
@ToString
public class Restaurant {

    @Id
    private String id;

    @Field("phone")
    private String phone;

    @Field("place_name")
    private String placeName;

    @Field("category_group_name")
    private String categoryGroupName;

    @Field("road_address_name")
    private String address;

    @Field("x")
    private String longitude;

    @Field("y")
    private String latitude;

    @Field("is_naver_available")
    private boolean isReservable;

    @Field("naver_url")
    private String naverUrl;
}
