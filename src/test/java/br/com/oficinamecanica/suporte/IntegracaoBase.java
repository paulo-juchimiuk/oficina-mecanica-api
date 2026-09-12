package br.com.oficinamecanica.suporte;

import br.com.oficinamecanica.shared.api.ApiPathPrefixConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
public abstract class IntegracaoBase {

    protected static final String PREFIXO = ApiPathPrefixConfig.PREFIXO;
    protected static final String LOGIN_DE_TESTE = "atendente.teste";
    protected static final String SENHA_DE_TESTE = "senha-de-teste-123";

    private static final String IMAGEM_DO_BANCO = "postgres:18-alpine";
    private static final String[] TABELAS = {
            "item_peca", "item_servico", "reserva_peca", "pendencia_peca", "transicao_status",
            "orcamento", "ordem_servico", "veiculo", "cliente", "peca", "servico", "usuario"};

    private static final PostgreSQLContainer BANCO = new PostgreSQLContainer(IMAGEM_DO_BANCO);

    static {
        BANCO.start();
    }

    @DynamicPropertySource
    static void apontarParaOContainer(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", BANCO::getJdbcUrl);
        registro.add("spring.datasource.username", BANCO::getUsername);
        registro.add("spring.datasource.password", BANCO::getPassword);
    }

    @MockitoBean
    protected MailSender mailSender;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @BeforeEach
    void prepararAmbiente() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", TABELAS) + " CASCADE");
    }

    protected void criarUsuarioDeTeste() {
        jdbc.update("INSERT INTO usuario (id, login, senha_hash, perfil) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), LOGIN_DE_TESTE, new BCryptPasswordEncoder().encode(SENHA_DE_TESTE), "ADMINISTRADOR");
    }

    protected String autenticar(String login, String senha) throws Exception {
        String corpo = mockMvc.perform(post(PREFIXO + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login": "%s", "senha": "%s"}
                                """.formatted(login, senha)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("token").asText();
    }

    protected List<SimpleMailMessage> emailsEnviados() {
        ArgumentCaptor<SimpleMailMessage> enviados = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(enviados.capture());
        return enviados.getAllValues();
    }

    protected List<SimpleMailMessage> emailsComAssunto(String assunto) {
        return emailsEnviados().stream().filter(mensagem -> assunto.equals(mensagem.getSubject())).toList();
    }

    protected String tokenAdministrativo() throws Exception {
        criarUsuarioDeTeste();
        return autenticar(LOGIN_DE_TESTE, SENHA_DE_TESTE);
    }
}
