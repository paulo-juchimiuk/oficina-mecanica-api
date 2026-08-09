package br.com.oficinamecanica.catalogo.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public class Servico {

    private static final String CODIGO = "SERVICO_INVALIDO";

    private final UUID id;
    private String nome;
    private String descricao;
    private Dinheiro valorMaoDeObra;
    private boolean ativo;

    public Servico(UUID id, String nome, String descricao, Dinheiro valorMaoDeObra, boolean ativo) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Servico exige identidade");
        }
        this.id = id;
        this.ativo = ativo;
        alterar(nome, descricao, valorMaoDeObra);
    }

    public static Servico cadastrar(String nome, String descricao, Dinheiro valorMaoDeObra) {
        return new Servico(UUID.randomUUID(), nome, descricao, valorMaoDeObra, true);
    }

    public final void alterar(String nome, String descricao, Dinheiro valorMaoDeObra) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException(CODIGO, "Servico exige nome");
        }
        if (valorMaoDeObra == null) {
            throw new DadosInvalidosException(CODIGO, "Servico exige Valor de mao de obra");
        }
        this.nome = nome.trim();
        this.descricao = semBrancos(descricao);
        this.valorMaoDeObra = valorMaoDeObra;
    }

    public void inativar() {
        this.ativo = false;
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public Dinheiro valorMaoDeObra() {
        return valorMaoDeObra;
    }

    public boolean ativo() {
        return ativo;
    }

    private String semBrancos(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
