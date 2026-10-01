package apitests;

import base.BaseTest;
import com.assureapi.model.Booking;
import com.assureapi.model.BookingDates;
import io.restassured.RestAssured;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import watcher.ConditionalRetryExtension;

import java.util.prefs.BackingStoreException;

public class BookingApiTest extends BaseTest {


    private static final int NON_EXISTENT_BOOKING_ID = 9999999;

    @Test
    public void testListingBooks() {
        Response response = RestAssured.given().get("/booking");
        Assertions.assertEquals(200,response.getStatusCode());
    }

    @Test
    @ConditionalRetryExtension.RetryOnTransientFailure(maxAttempts = 3)
    public void testGetBookingParam(){
        Response response = getBookingById(2);
        Assertions.assertEquals(200,response.getStatusCode());
        Booking booking = response.as(Booking.class);
        Assertions.assertNotNull(booking.getFirstname());
        Assertions.assertNotNull(booking.getLastname());
        Assertions.assertTrue(booking.getTotalprice() > 0);
   }

    @Test
    @ConditionalRetryExtension.RetryOnTransientFailure(maxAttempts = 3)
    public void testCreateNewBooking(){
        Booking novaReserva = new Booking();
        novaReserva.setFirstname("Americo");
        novaReserva.setLastname("Silva");
        novaReserva.setTotalprice(100);
        novaReserva.setDepositpaid(true);

        BookingDates datas = new BookingDates();
        datas.setCheckin("2023-01-01");
        datas.setCheckout("2023-01-10");
        novaReserva.setBookingdates(datas);
        novaReserva.setAdditionalneeds("Breakfast");

        Response  response = RestAssured.given()
                .header("Content-Type","application/json")
                .body(novaReserva)
                .post("/booking");
        Assertions.assertEquals(200,response.getStatusCode());

        Booking criadaReserva = response.jsonPath().getObject("booking", Booking.class);
        Assertions.assertEquals(novaReserva.getFirstname(),criadaReserva.getFirstname());
        Assertions.assertEquals(novaReserva.getLastname(),criadaReserva.getLastname());
        Assertions.assertEquals(novaReserva.getTotalprice(),criadaReserva.getTotalprice());
        Assertions.assertEquals(novaReserva.isDepositpaid(),criadaReserva.isDepositpaid());


    }


    @Test
    public void testBookingNotFound(){
        Response response = getBookingById(NON_EXISTENT_BOOKING_ID);
        Assertions.assertEquals(404, response.getStatusCode());

    }


    public static Response getBookingById(int id){
        Response response = RestAssured
                .given()
                .pathParam("id",id)
                .when()
                .get("/booking/{id}")
                .then()
                .extract().response();
        return response;
    }

}
