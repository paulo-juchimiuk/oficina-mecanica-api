package br.com.oficinamecanica.shared.infrastructure;

import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import java.lang.reflect.Method;
import java.util.Set;

class AtributosDeTransacaoDosCasosDeUso implements TransactionAttributeSource {

    private static final String SUFIXO_DO_CASO_DE_USO = "UseCase";

    private static final Set<String> FORA_DE_TRANSACAO = Set.of(
            "AutenticarUsuarioUseCase");

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

    @Override
    public boolean isCandidateClass(Class<?> alvo) {
        return alvo.getSimpleName().endsWith(SUFIXO_DO_CASO_DE_USO);
    }

    @Override
    public TransactionAttribute getTransactionAttribute(Method metodo, Class<?> alvo) {
        String nome = classeDeclarada(metodo, alvo).getSimpleName();
        if (!nome.endsWith(SUFIXO_DO_CASO_DE_USO)) {
            return null;
        }
        if (FORA_DE_TRANSACAO.contains(nome)) {
            return null;
        }
        DefaultTransactionAttribute atributo = new DefaultTransactionAttribute();
        atributo.setReadOnly(SOMENTE_LEITURA.contains(nome));
        atributo.setName(nome + "." + metodo.getName());
        return atributo;
    }

    private Class<?> classeDeclarada(Method metodo, Class<?> alvo) {
        if (alvo == null) {
            return metodo.getDeclaringClass();
        }
        return alvo;
    }
}
