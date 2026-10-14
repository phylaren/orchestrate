package genius.project.orchestrate.identity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Наскрізна перевірка безпеки на повному контексті з БД, справжнім Argon2id та JWT:
 * без токена → 401; вхід → токен; з токеном → 200; без прав → 403.
 */
@SpringBootTest
@Transactional
class SecurityFlowIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Сценарій 401 → вхід → 200 → 403 для власника і звичайного мешканця")
    void fullSecurityScenario() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String ownerEmail = "owner." + runId + "@orchestrate.test";
        String residentEmail = "resident." + runId + "@orchestrate.test";
        register("Owner", ownerEmail);
        register("Resident", residentEmail);

        // 1. Без токена — 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        // 2. Вхід — токен доступу
        String ownerToken = login(ownerEmail);
        String residentToken = login(residentEmail);

        // 3. З токеном — 200
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ownerEmail));

        String householdId = createHouseholdWithResident(ownerToken, residentToken);
        String permissionsUrl = "/api/v1/households/" + householdId + "/members/permissions";

        mockMvc.perform(get(permissionsUrl).header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));

        // 4. Користувач без права MANAGE_MEMBERS — 403
        mockMvc.perform(get(permissionsUrl).header(HttpHeaders.AUTHORIZATION, bearer(residentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Невірний пароль при вході — 401 INVALID_CREDENTIALS")
    void login_WrongPassword_Returns401() throws Exception {
        String email = "wrong." + UUID.randomUUID().toString().substring(0, 8) + "@orchestrate.test";
        register("Wrong", email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"not-the-password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Підроблений токен — 401 INVALID_TOKEN")
    void tamperedToken_Returns401() throws Exception {
        String email = "tamper." + UUID.randomUUID().toString().substring(0, 8) + "@orchestrate.test";
        register("Tamper", email);
        String token = login(email);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("TraceIdFilter зареєстровано раніше за ланцюг Spring Security")
    void traceIdFilter_RunsBeforeSecurityChain() {
        int traceOrder = context.getBean("traceIdFilterRegistration", AbstractFilterRegistrationBean.class).getOrder();
        int securityOrder = context.getBean("securityFilterChainRegistration", AbstractFilterRegistrationBean.class).getOrder();

        assertThat(traceOrder).isLessThan(securityOrder);
    }

    private void register(String displayName, String email) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","email":"%s","password":"%s"}
                                """.formatted(displayName, email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String createHouseholdWithResident(String ownerToken, String residentToken) throws Exception {
        String household = mockMvc.perform(post("/api/v1/households")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Квартира"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String householdId = JsonPath.read(household, "$.id");

        String invitation = mockMvc.perform(post("/api/v1/households/" + householdId + "/invitation-code")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(invitation, "$.code");

        mockMvc.perform(post("/api/v1/households/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(residentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
                .andExpect(status().isCreated());
        return householdId;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}