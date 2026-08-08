package br.com.oficinamecanica.cadastro.domain;

import br.com.oficinamecanica.shared.domain.DadosInvalidosException;
import java.util.UUID;

public class Cliente {

    private static final String CODIGO = "CLIENTE_INVALIDO";

    private final UUID id;
    private String nome;
    private Documento documento;
    private Contato contato;
    private boolean ativo;

    public Cliente(UUID id, String nome, Documento documento, Contato contato, boolean ativo) {
        if (id == null) {
            throw new DadosInvalidosException(CODIGO, "Cliente exige identidade");
        }
        this.id = id;
        this.ativo = ativo;
        alterar(nome, documento, contato);
    }

    public static Cliente cadastrar(String nome, Documento documento, Contato contato) {
        return new Cliente(UUID.randomUUID(), nome, documento, contato, true);
    }

    public final void alterar(String nome, Documento documento, Contato contato) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException(CODIGO, "Cliente exige nome");
        }
        if (documento == null) {
            throw new DadosInvalidosException(CODIGO, "Cliente exige Documento");
        }
        if (contato == null) {
            throw new DadosInvalidosException(CODIGO, "Cliente exige Contato");
        }
        this.nome = nome.trim();
        this.documento = documento;
        this.contato = contato;
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

    public Documento documento() {
        return documento;
    }

    public Contato contato() {
        return contato;
    }

    public boolean ativo() {
        return ativo;
    }
}
