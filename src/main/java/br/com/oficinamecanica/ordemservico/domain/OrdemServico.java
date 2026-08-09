package br.com.oficinamecanica.ordemservico.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrdemServico {

    private static final String CODIGO = "ORDEM_SERVICO_INVALIDA";
    private static final int TAMANHO_MAXIMO_DO_RELATO = 1000;
    private static final StatusOrdemServico STATUS_INICIAL = StatusOrdemServico.RECEBIDA;

    private final UUID id;
    private final UUID clienteId;
    private final UUID veiculoId;
    private final CodigoAcompanhamento codigoAcompanhamento;
    private final String relatoDoProblema;
    private final LocalDateTime criadaEm;
    private final List<TransicaoStatus> transicoes;
    private StatusOrdemServico status;

    public OrdemServico(UUID id, UUID clienteId, UUID veiculoId, StatusOrdemServico status,
                        CodigoAcompanhamento codigoAcompanhamento, String relatoDoProblema,
                        LocalDateTime criadaEm, List<TransicaoStatus> transicoes) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige identidade");
        }
        if (clienteId == null || veiculoId == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige Cliente e Veiculo");
        }
        if (status == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige Status da OS");
        }
        if (codigoAcompanhamento == null) {
            throw new DadosInvalidosException(CODIGO, "Ordem de Servico exige Codigo de acompanhamento");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = status;
        this.codigoAcompanhamento = codigoAcompanhamento;
        this.relatoDoProblema = relatoValidado(relatoDoProblema);
        this.criadaEm = criadaEm;
        this.transicoes = new ArrayList<>(transicoes);
    }

    public static OrdemServico abrir(UUID clienteId, UUID veiculoId, String relatoDoProblema) {
        return new OrdemServico(UUID.randomUUID(), clienteId, veiculoId, STATUS_INICIAL,
                CodigoAcompanhamento.gerar(), relatoDoProblema, LocalDateTime.now(),
                List.of(TransicaoStatus.abertura(STATUS_INICIAL)));
    }

    public void iniciarDiagnostico() {
        transicionarPara(StatusOrdemServico.EM_DIAGNOSTICO);
    }

    private void transicionarPara(StatusOrdemServico destino) {
        if (!status.aceitaTransicaoPara(destino)) {
            throw new TransicaoInvalidaException(id, status, destino);
        }
        transicoes.add(TransicaoStatus.entre(status, destino));
        this.status = destino;
    }

    private String relatoValidado(String relato) {
        if (relato == null || relato.isBlank()) {
            return null;
        }
        String limpo = relato.trim();
        if (limpo.length() > TAMANHO_MAXIMO_DO_RELATO) {
            throw new DadosInvalidosException(CODIGO,
                    "Relato do problema excede " + TAMANHO_MAXIMO_DO_RELATO + " caracteres");
        }
        return limpo;
    }

    public UUID id() {
        return id;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public UUID veiculoId() {
        return veiculoId;
    }

    public StatusOrdemServico status() {
        return status;
    }

    public CodigoAcompanhamento codigoAcompanhamento() {
        return codigoAcompanhamento;
    }

    public String relatoDoProblema() {
        return relatoDoProblema;
    }

    public LocalDateTime criadaEm() {
        return criadaEm;
    }

    public List<TransicaoStatus> transicoes() {
        return List.copyOf(transicoes);
    }
}
