#!/bin/sh

ARQUIVO_DE_CARGA="$(dirname "$0")/dados-demonstracao.sql"
TENTATIVAS_MAXIMAS=120

tentativas=0
until [ "$(psql -tAc "select to_regclass('public.ordem_servico') is not null")" = "t" ]; do
    tentativas=$((tentativas + 1))
    if [ "$tentativas" -ge "$TENTATIVAS_MAXIMAS" ]; then
        echo "seed: o schema nao apareceu em $TENTATIVAS_MAXIMAS tentativas; o Flyway do app nao rodou" >&2
        exit 1
    fi
    sleep 1
done

existentes=$(psql -tAc 'select count(*) from ordem_servico') || {
    echo "seed: nao foi possivel consultar o banco para decidir se ja esta populado" >&2
    exit 1
}

if [ "$existentes" != "0" ]; then
    echo "seed: o banco ja esta populado, nada a fazer"
    exit 0
fi

psql --single-transaction -v ON_ERROR_STOP=1 -q -f "$ARQUIVO_DE_CARGA" \
    && echo "seed: dados de demonstracao carregados"
