package genius.project.orchestrate.user;

import genius.project.orchestrate.user.dto.UserCreateRequest;
import genius.project.orchestrate.user.dto.UserResponse;
import genius.project.orchestrate.user.internal.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        User user = userService.createUser(request.displayName(), request.email());
        URI location = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(user.id()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable UUID userId) {
        return UserResponse.from(userService.getUser(userId));
    }
}
