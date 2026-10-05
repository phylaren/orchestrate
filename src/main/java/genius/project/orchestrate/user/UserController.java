package genius.project.orchestrate.user;

import genius.project.orchestrate.user.dto.UserCreateRequest;
import genius.project.orchestrate.user.dto.UserResponse;
import genius.project.orchestrate.user.internal.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Користувачі", description = "Облікові записи користувачів системи")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Створити користувача",
            description = "Реєструє нового користувача. Електронна пошта має бути унікальною.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Користувача створено; заголовок Location вказує на новий ресурс",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некоректні вхідні дані (VALIDATION_FAILED)"),
            @ApiResponse(responseCode = "409", description = "Електронна пошта вже зайнята (EMAIL_ALREADY_TAKEN)")
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        User user = userService.createUser(request.displayName(), request.email());
        URI location = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(user.id()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    @Operation(summary = "Отримати список користувачів",
            description = "Повертає всіх користувачів, відсортованих за часом створення.")
    @ApiResponse(responseCode = "200", description = "Список користувачів")
    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Operation(summary = "Отримати користувача за ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Користувача знайдено"),
            @ApiResponse(responseCode = "404", description = "Користувача не знайдено (USER_NOT_FOUND)")
    })
    @GetMapping("/{userId}")
    public UserResponse getUser(
            @Parameter(description = "Ідентифікатор користувача", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID userId) {
        return UserResponse.from(userService.getUser(userId));
    }
}
