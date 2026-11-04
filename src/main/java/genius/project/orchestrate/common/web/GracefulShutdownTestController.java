package genius.project.orchestrate.common.web;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/__test")
@ConditionalOnProperty(name = "orchestrate.test-endpoints.enabled", havingValue = "true")
class GracefulShutdownTestController {
    private static final int MAX_DELAY_SECONDS = 25;
    @GetMapping("/slow")
    ResponseEntity<String> slow(@RequestParam(defaultValue = "15") int seconds) {
        if (seconds < 1 || seconds > MAX_DELAY_SECONDS) {
            return ResponseEntity.badRequest().body("seconds must be between 1 and 25");
        }
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Slow test request was interrupted", exception);
        }
        String unit = seconds == 1 ? "second" : "seconds";
        return ResponseEntity.ok("Completed after " + seconds + " " + unit);
    }
}