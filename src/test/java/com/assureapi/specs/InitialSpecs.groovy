package com.assureapi.specs

import io.restassured.builder.RequestSpecBuilder
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification

public class InitialSpecs {

    public static RequestSpecification setuRequest(){
        return new RequestSpecBuilder()
                .setBaseUri("https://restful-booker.herokuapp.com")
                .setContentType("application/json")
                .build();
    }


    public static ResponseSpecification setResponse(){
        return new ResponseSpecBuilder()
                .expectHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }



}



