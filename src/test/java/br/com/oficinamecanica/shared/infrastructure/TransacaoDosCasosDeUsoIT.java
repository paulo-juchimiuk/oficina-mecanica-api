package br.com.oficinamecanica.shared.infrastructure;

import br.com.oficinamecanica.suporte.IntegracaoBase;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transacao dos casos de uso declarada na infraestrutura")
class TransacaoDosCasosDeUsoIT extends IntegracaoBase {

    private static final String PACOTE_RAIZ = "br.com.oficinamecanica";
    private static final String PACOTE_DOS_CASOS_DE_USO = ".application";
    private static final String SUFIXO_DO_CASO_DE_USO = "UseCase";
    private static final String METODO_DO_CASO_DE_USO = "executar";

    private static final int TOTAL_DE_CASOS_DE_USO = 41;
    private static final Set<String> SEM_TRANSACAO = Set.of("AutenticarUsuarioUseCase");
    private static final Set<String> SOMENTE_LEITURA = Set.of(
            "ConsultarAcompanhamentoUseCase",
            "ConsultarPecasReservadasUseCase",
            "ConsultarPendenciasDePecasUseCase",
            "ConsultarTempoMedioExecucaoUseCase",
            "DetalharClienteUseCase",
            "DetalharOrdemServicoUseCase",
            "DetalharPecaUseCase",
            "DetalharServicoUseCase",
            "DetalharVeiculoUseCase",
            "ListarClientesUseCase",
            "ListarOrdensServicoUseCase",
            "ListarPecasUseCase",
            "ListarServicosUseCase",
            "ListarVeiculosUseCase");

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("deve registrar como bean cada caso de uso que existe no codigo")
    void deveRegistrarCadaCasoDeUsoQueExisteNoCodigo() {
        Set<String> noCodigo = casosDeUsoDeclaradosNoCodigo();

        assertThat(noCodigo).hasSize(TOTAL_DE_CASOS_DE_USO);
        assertThat(atributosPorCasoDeUso().keySet()).isEqualTo(noCodigo);
    }

    @Test
    @DisplayName("deve deixar apenas a autenticacao fora de transacao")
    void deveDeixarApenasAAutenticacaoForaDeTransacao() {
        Map<String, TransactionAttribute> atributos = atributosPorCasoDeUso();

        Set<String> foraDeTransacao = atributos.entrySet().stream()
                .filter(caso -> caso.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(toSet());

        assertThat(foraDeTransacao).isEqualTo(SEM_TRANSACAO);
        assertThat(atributos).hasSize(TOTAL_DE_CASOS_DE_USO);
    }

    @Test
    @DisplayName("deve declarar somente leitura apenas nas consultas")
    void deveDeclararSomenteLeituraApenasNasConsultas() {
        Set<String> somenteLeitura = atributosPorCasoDeUso().entrySet().stream()
                .filter(caso -> caso.getValue() != null && caso.getValue().isReadOnly())
                .map(Map.Entry::getKey)
                .collect(toSet());

        assertThat(somenteLeitura).isEqualTo(SOMENTE_LEITURA);
    }

    private Set<String> casosDeUsoDeclaradosNoCodigo() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(PACOTE_RAIZ)
                .stream()
                .filter(classe -> classe.getPackageName().endsWith(PACOTE_DOS_CASOS_DE_USO))
                .filter(classe -> classe.getSimpleName().endsWith(SUFIXO_DO_CASO_DE_USO))
                .map(JavaClass::getSimpleName)
                .collect(toSet());
    }

    private Map<String, TransactionAttribute> atributosPorCasoDeUso() {
        Map<String, TransactionAttribute> atributos = new HashMap<>();
        for (String nome : contexto.getBeanDefinitionNames()) {
            Object bean = contexto.getBean(nome);
            Class<?> alvo = AopUtils.getTargetClass(bean);
            if (!alvo.getSimpleName().endsWith(SUFIXO_DO_CASO_DE_USO)) {
                continue;
            }
            atributos.put(alvo.getSimpleName(), atributoDeTransacao(bean, alvo));
        }
        return atributos;
    }

    private TransactionAttribute atributoDeTransacao(Object bean, Class<?> alvo) {
        if (!(bean instanceof Advised proxy)) {
            return null;
        }
        return interceptador(proxy)
                .map(interceptador -> interceptador.getTransactionAttributeSource()
                        .getTransactionAttribute(metodoDoCasoDeUso(alvo), alvo))
                .orElse(null);
    }

    private Optional<TransactionInterceptor> interceptador(Advised proxy) {
        return Arrays.stream(proxy.getAdvisors())
                .map(Advisor::getAdvice)
                .filter(TransactionInterceptor.class::isInstance)
                .map(TransactionInterceptor.class::cast)
                .findFirst();
    }

    private Method metodoDoCasoDeUso(Class<?> alvo) {
        return Arrays.stream(alvo.getMethods())
                .filter(metodo -> metodo.getName().equals(METODO_DO_CASO_DE_USO))
                .findFirst()
                .orElseThrow();
    }
}
