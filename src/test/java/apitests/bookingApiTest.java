package apitests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;

public class bookingApiTest {

    public static String BASEURI = "https://restful-booker.herokuapp.com";

    @Test
    public void testListingBooks() {
        Response response = RestAssured.given()
                .baseUri(BASEURI)
                .get("/booking");
        Assert.assertEquals(response.getStatusCode(), 200);
    }

    @Test
    public void testGetBookingParam(){
        Response response = getBookingById(1);
        Assert.assertEquals(response.getStatusCode(),200);
        System.out.println(response.asString());
    }

    public static Response getBookingById(int id){
        Response response = RestAssured
                .given()
                .pathParam("id",id)
                .when()
                .get(BASEURI+"/booking/{id}")
                .then()
                .extract().response();
        return response;
    }

}
