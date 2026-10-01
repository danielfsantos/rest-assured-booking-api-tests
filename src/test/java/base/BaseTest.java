package base;

import com.assureapi.utils.ConfigReader;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import watcher.TestResultWatcher;


@ExtendWith(TestResultWatcher.class)
public class BaseTest {

    @BeforeAll
    public static void setup(){
        RestAssured.baseURI = ConfigReader.getBaseUrl();
    }


}
