package semantico;

import sintatico.NoSintatico;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalisadorSemantico {
    private enum TipoDado {
        INTEIRO,
        DECIMAL,
        TEXTO,
        BOOLEANO,
        INVALIDO
    }

    private final Deque<Map<String, TipoDado>> escopos = new ArrayDeque<>();
    private final List<String> erros = new ArrayList<>();

    public void analisar(NoSintatico raiz) {
        erros.clear();
        escopos.clear();
        entrarEscopo();
        visitar(raiz);
        sairEscopo();
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    public List<String> getErros() {
        return erros;
    }

    private void entrarEscopo() {
        escopos.push(new HashMap<>());
    }

    private void sairEscopo() {
        if (!escopos.isEmpty()) {
            escopos.pop();
        }
    }

    private Map<String, TipoDado> escopoAtual() {
        return escopos.peek();
    }

    private void declarar(String nome, TipoDado tipo, NoSintatico ref) {
        if (nome == null || tipo == null) {
            return;
        }
        Map<String, TipoDado> atual = escopoAtual();
        if (atual != null && atual.containsKey(nome)) {
            addErro("Variavel '" + nome + "' ja declarada neste escopo", ref);
            return;
        }
        if (atual != null) {
            atual.put(nome, tipo);
        }
    }

    private TipoDado buscar(String nome, NoSintatico ref) {
        if (nome == null) {
            return TipoDado.INVALIDO;
        }
        for (Map<String, TipoDado> escopo : escopos) {
            if (escopo.containsKey(nome)) {
                return escopo.get(nome);
            }
        }
        addErro("Variavel '" + nome + "' nao declarada", ref);
        return TipoDado.INVALIDO;
    }

    private void addErro(String msg, NoSintatico ref) {
        int linha = obterLinha(ref);
        addErro(msg, linha);
    }

    private void addErro(String msg, int linha) {
        if (linha > 0) {
            erros.add("Erro semantico (linha " + linha + "): " + msg);
        } else {
            erros.add("Erro semantico: " + msg);
        }
    }

    private int obterLinha(NoSintatico no) {
        if (no == null) {
            return -1;
        }
        if (no.getLinha() > 0) {
            return no.getLinha();
        }
        for (NoSintatico filho : no.getFilhos()) {
            int linha = obterLinha(filho);
            if (linha > 0) {
                return linha;
            }
        }
        return -1;
    }

    private void visitar(NoSintatico no) {
        if (no == null) {
            return;
        }
        String rotulo = no.getRotulo();
        switch (rotulo) {
            case "S":
                for (NoSintatico filho : no.getFilhos()) {
                    visitar(filho);
                }
                break;
            case "bloco":
                visitarBloco(no);
                break;
            case "cmd":
                for (NoSintatico filho : no.getFilhos()) {
                    visitar(filho);
                }
                break;
            case "declaracao":
                processarDeclaracao(no);
                break;
            case "declaracao_para":
                processarDeclaracaoPara(no);
                break;
            case "atribuicao":
                processarAtribuicao(no);
                break;
            case "atribuicao_interna":
                processarAtribuicaoInterna(no);
                break;
            case "leitura":
                processarLeitura(no);
                break;
            case "escrita":
                processarEscrita(no);
                break;
            case "se":
                processarSe(no);
                break;
            case "sns":
                processarSns(no);
                break;
            case "enquanto":
                processarEnquanto(no);
                break;
            case "para":
                processarPara(no);
                break;
            default:
                for (NoSintatico filho : no.getFilhos()) {
                    visitar(filho);
                }
                break;
        }
    }

    private void visitarBloco(NoSintatico no) {
        entrarEscopo();
        for (NoSintatico filho : no.getFilhos()) {
            visitar(filho);
        }
        sairEscopo();
    }

    private void processarDeclaracao(NoSintatico no) {
        TipoDado tipo = null;
        String nome = null;
        NoSintatico idNo = null;

        for (NoSintatico filho : no.getFilhos()) {
            if ("tipo".equals(filho.getRotulo())) {
                tipo = tipoDoNoTipo(filho);
            } else if ("ID".equals(filho.getRotulo())) {
                nome = filho.getValor();
                idNo = filho;
            }
        }

        if (tipo != null && nome != null) {
            declarar(nome, tipo, idNo);
        }
    }

    private void processarDeclaracaoPara(NoSintatico no) {
        TipoDado tipo = null;
        String nome = null;
        NoSintatico expr = null;
        NoSintatico idNo = null;

        for (NoSintatico filho : no.getFilhos()) {
            if ("tipo".equals(filho.getRotulo())) {
                tipo = tipoDoNoTipo(filho);
            } else if ("ID".equals(filho.getRotulo())) {
                nome = filho.getValor();
                idNo = filho;
            } else if (isExpressaoNo(filho)) {
                expr = filho;
            }
        }

        if (tipo != null && nome != null) {
            declarar(nome, tipo, idNo);
        }

        if (tipo != null && expr != null && nome != null) {
            TipoDado tipoExpr = resolverTipoExpressao(expr);
            verificarCompatibilidadeAtribuicao(nome, tipo, tipoExpr, idNo);
        }
    }

    private void processarAtribuicao(NoSintatico no) {
        String nome = null;
        NoSintatico expr = null;
        NoSintatico idNo = null;

        for (NoSintatico filho : no.getFilhos()) {
            if ("ID".equals(filho.getRotulo())) {
                nome = filho.getValor();
                idNo = filho;
            } else if (isExpressaoNo(filho)) {
                expr = filho;
            }
        }

        if (nome == null) {
            return;
        }

        TipoDado tipoVar = buscar(nome, idNo);
        TipoDado tipoExpr = expr != null ? resolverTipoExpressao(expr) : TipoDado.INVALIDO;
        verificarCompatibilidadeAtribuicao(nome, tipoVar, tipoExpr, idNo);
    }

    private void processarAtribuicaoInterna(NoSintatico no) {
        processarAtribuicao(no);
    }

    private void processarLeitura(NoSintatico no) {
        String nome = null;
        NoSintatico idNo = null;
        for (NoSintatico filho : no.getFilhos()) {
            if ("ID".equals(filho.getRotulo())) {
                nome = filho.getValor();
                idNo = filho;
                break;
            }
        }
        if (nome != null) {
            buscar(nome, idNo);
        }
    }

    private void processarEscrita(NoSintatico no) {
        for (NoSintatico filho : no.getFilhos()) {
            if ("conteudo".equals(filho.getRotulo())) {
                for (NoSintatico c : filho.getFilhos()) {
                    if ("ID".equals(c.getRotulo())) {
                        buscar(c.getValor(), c);
                        return;
                    }
                }
            }
        }
    }

    private void processarSe(NoSintatico no) {
        NoSintatico condicao = encontrarPrimeiraExpressao(no);
        checarCondicao(condicao, "se");

        for (NoSintatico filho : no.getFilhos()) {
            if ("bloco".equals(filho.getRotulo())) {
                visitar(filho);
            } else if ("sns".equals(filho.getRotulo())) {
                processarSns(filho);
            }
        }
    }

    private void processarSns(NoSintatico no) {
        if (no.getFilhos().isEmpty()) {
            return;
        }

        NoSintatico primeiro = no.getFilhos().get(0);
        if ("\u03b5".equals(primeiro.getRotulo())) {
            return;
        }

        if ("SENAO_SE".equals(primeiro.getRotulo())) {
            NoSintatico condicao = encontrarPrimeiraExpressao(no);
            checarCondicao(condicao, "senaose");

            for (NoSintatico filho : no.getFilhos()) {
                if ("bloco".equals(filho.getRotulo())) {
                    visitar(filho);
                } else if ("sns".equals(filho.getRotulo())) {
                    processarSns(filho);
                }
            }
            return;
        }

        if ("SENAO".equals(primeiro.getRotulo())) {
            for (NoSintatico filho : no.getFilhos()) {
                if ("bloco".equals(filho.getRotulo())) {
                    visitar(filho);
                }
            }
        }
    }

    private void processarEnquanto(NoSintatico no) {
        NoSintatico condicao = encontrarPrimeiraExpressao(no);
        checarCondicao(condicao, "enquanto");

        for (NoSintatico filho : no.getFilhos()) {
            if ("bloco".equals(filho.getRotulo())) {
                visitar(filho);
            }
        }
    }

    private void processarPara(NoSintatico no) {
        entrarEscopo();
        boolean condicaoChecada = false;

        for (NoSintatico filho : no.getFilhos()) {
            String rotulo = filho.getRotulo();
            if ("declaracao_para".equals(rotulo)) {
                processarDeclaracaoPara(filho);
            } else if ("atribuicao_interna".equals(rotulo)) {
                processarAtribuicaoInterna(filho);
            } else if ("bloco".equals(rotulo)) {
                visitar(filho);
            } else if (!condicaoChecada && isExpressaoNo(filho)) {
                checarCondicao(filho, "para");
                condicaoChecada = true;
            }
        }

        sairEscopo();
    }

    private TipoDado tipoDoNoTipo(NoSintatico no) {
        for (NoSintatico filho : no.getFilhos()) {
            switch (filho.getRotulo()) {
                case "INTEIRO":
                    return TipoDado.INTEIRO;
                case "DECIMAL":
                    return TipoDado.DECIMAL;
                case "TEXTO":
                    return TipoDado.TEXTO;
                default:
                    break;
            }
        }
        return null;
    }

    private boolean isExpressaoNo(NoSintatico no) {
        if (no == null) {
            return false;
        }
        String r = no.getRotulo();
        return r.equals("+") || r.equals("-") || r.equals("*") || r.equals("/") ||
               r.equals("op_rel") || r.equals("||") || r.equals("&&") ||
               r.equals("fator") || r.equals("ID") || r.equals("NUM_INT") ||
               r.equals("NUM_DECIMAL") || r.equals("TEXTO");
    }

    private NoSintatico encontrarPrimeiraExpressao(NoSintatico no) {
        for (NoSintatico filho : no.getFilhos()) {
            if (isExpressaoNo(filho)) {
                return filho;
            }
        }
        return null;
    }

    private void checarCondicao(NoSintatico no, String contexto) {
        if (no == null) {
            return;
        }
        TipoDado tipo = resolverTipoExpressao(no);
        if (tipo != TipoDado.INVALIDO && tipo != TipoDado.BOOLEANO) {
            addErro("Condicao do '" + contexto + "' deve ser booleana", no);
        }
    }

    private void verificarCompatibilidadeAtribuicao(String nome, TipoDado tipoVar, TipoDado tipoExpr, NoSintatico ref) {
        if (tipoVar == TipoDado.INVALIDO || tipoExpr == TipoDado.INVALIDO) {
            return;
        }
        if (tipoVar != tipoExpr) {
            addErro("Tipos incompativeis na atribuicao de '" + nome + "': esperado " +
                    tipoParaTexto(tipoVar) + ", encontrado " + tipoParaTexto(tipoExpr), ref);
        }
    }

    private TipoDado resolverTipoExpressao(NoSintatico no) {
        if (no == null) {
            return TipoDado.INVALIDO;
        }
        String rotulo = no.getRotulo();

        switch (rotulo) {
            case "NUM_INT":
                return TipoDado.INTEIRO;
            case "NUM_DECIMAL":
                return TipoDado.DECIMAL;
            case "TEXTO":
                return TipoDado.TEXTO;
            case "ID":
                return buscar(no.getValor(), no);
            case "fator":
                for (NoSintatico filho : no.getFilhos()) {
                    return resolverTipoExpressao(filho);
                }
                return TipoDado.INVALIDO;
            case "+":
            case "-":
            case "*":
            case "/":
                return resolverTipoAritmetico(no, rotulo);
            case "op_rel":
                return resolverTipoRelacional(no);
            case "&&":
            case "||":
                return resolverTipoLogico(no, rotulo);
            default:
                if (!no.getFilhos().isEmpty()) {
                    for (NoSintatico filho : no.getFilhos()) {
                        TipoDado tipo = resolverTipoExpressao(filho);
                        if (tipo != TipoDado.INVALIDO) {
                            return tipo;
                        }
                    }
                }
                return TipoDado.INVALIDO;
        }
    }

    private TipoDado resolverTipoAritmetico(NoSintatico no, String op) {
        if (no.getFilhos().size() < 2) {
            return TipoDado.INVALIDO;
        }

        TipoDado esq = resolverTipoExpressao(no.getFilhos().get(0));
        TipoDado dir = resolverTipoExpressao(no.getFilhos().get(1));

        if (esq == TipoDado.INVALIDO || dir == TipoDado.INVALIDO) {
            return TipoDado.INVALIDO;
        }

        if (!isNumerico(esq) || !isNumerico(dir)) {
            addErro("Operador '" + op + "' exige operandos numericos", no);
            return TipoDado.INVALIDO;
        }

        if (esq != dir) {
            addErro("Tipos incompativeis em expressao aritmetica", no);
            return TipoDado.INVALIDO;
        }

        return esq;
    }

    private TipoDado resolverTipoRelacional(NoSintatico no) {
        if (no.getFilhos().size() < 2) {
            return TipoDado.INVALIDO;
        }

        TipoDado esq = resolverTipoExpressao(no.getFilhos().get(0));
        TipoDado dir = resolverTipoExpressao(no.getFilhos().get(1));

        if (esq == TipoDado.INVALIDO || dir == TipoDado.INVALIDO) {
            return TipoDado.INVALIDO;
        }

        String op = no.getValor();
        if (op != null && (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<="))) {
            if (!isNumerico(esq) || !isNumerico(dir)) {
                addErro("Operador relacional '" + op + "' exige operandos numericos", no);
                return TipoDado.INVALIDO;
            }
        }

        if (esq != dir) {
            addErro("Tipos incompativeis em expressao relacional", no);
            return TipoDado.INVALIDO;
        }

        return TipoDado.BOOLEANO;
    }

    private TipoDado resolverTipoLogico(NoSintatico no, String op) {
        if (no.getFilhos().size() < 2) {
            return TipoDado.INVALIDO;
        }

        TipoDado esq = resolverTipoExpressao(no.getFilhos().get(0));
        TipoDado dir = resolverTipoExpressao(no.getFilhos().get(1));

        if (esq == TipoDado.INVALIDO || dir == TipoDado.INVALIDO) {
            return TipoDado.INVALIDO;
        }

        if (esq != TipoDado.BOOLEANO || dir != TipoDado.BOOLEANO) {
            addErro("Operador logico '" + op + "' exige operandos booleanos", no);
            return TipoDado.INVALIDO;
        }

        return TipoDado.BOOLEANO;
    }

    private boolean isNumerico(TipoDado tipo) {
        return tipo == TipoDado.INTEIRO || tipo == TipoDado.DECIMAL;
    }

    private String tipoParaTexto(TipoDado tipo) {
        switch (tipo) {
            case INTEIRO:
                return "inteiro";
            case DECIMAL:
                return "decimal";
            case TEXTO:
                return "texto";
            case BOOLEANO:
                return "booleano";
            default:
                return "invalido";
        }
    }
}
