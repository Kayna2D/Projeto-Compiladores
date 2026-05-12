package gerador;

import sintatico.NoSintatico;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GeradorCodigoC {
    private StringBuilder codigoC;
    private Map<String, String> tabelaSimbolos;
    private int nivelIndentacao;

    public GeradorCodigoC() {
        codigoC = new StringBuilder();
        tabelaSimbolos = new HashMap<>();
        nivelIndentacao = 0;
    }

    public String gerarCodigo(NoSintatico raiz) {
        codigoC.append("#include <stdio.h>\n");
        codigoC.append("#include <string.h>\n\n");
        codigoC.append("int main() {\n");

        nivelIndentacao++;

        if (raiz.getRotulo().equals("S")) {
            for (NoSintatico filho : raiz.getFilhos()) {
                processarNo(filho);
            }
        }

        codigoC.append("    return 0;\n");
        codigoC.append("}\n");

        return codigoC.toString();
    }

    private void identar() {
        for (int i = 0; i < nivelIndentacao; i++) {
            codigoC.append("    ");
        }
    }

    private void processarNo(NoSintatico no) {
        if (no == null) return;

        switch (no.getRotulo()) {
            case "INICIO":
            case "FIM":
            case "ε":    
                break;
            case "bloco":
                processarBloco(no);
                break;
            case "cmd":
                processarCmd(no);
                break;
            case "declaracao":
                processarDeclaracao(no);
                break;
            case "atribuicao":
                processarAtribuicao(no);
                break;
            case "escrita":
                processarEscrita(no);
                break;
            case "leitura":
                processarLeitura(no);
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
                    processarNo(filho);
                }
                break;
        }
    }

    private void processarBloco(NoSintatico no) {
        for (NoSintatico filho : no.getFilhos()) {
            if (!filho.getRotulo().equals("ε")) {
                processarNo(filho);                
            }
        }
    }

    private void processarCmd(NoSintatico no) {
        for (NoSintatico filho : no.getFilhos()) {
            processarNo(filho);
        }
    }

    private void processarDeclaracao(NoSintatico no) {
        String tipoC = null;
        String nomeVar = null;
        String tipoOriginal = null;

        for (NoSintatico filho : no.getFilhos()) {
            switch (filho.getRotulo()) {
                case "tipo":
                    tipoOriginal = processarTipo(filho);
                    tipoC = converterTipoC(tipoOriginal);
                    break;
                case "ID":
                    nomeVar = filho.getValor();
                    break;
            }
        }

        if (tipoC != null && nomeVar != null) {
            tabelaSimbolos.put(nomeVar, tipoOriginal);
            identar();
            if (tipoC.equals("char[]")) {
                codigoC.append("char ").append(nomeVar).append("[100];\n");
            } else {
                codigoC.append(tipoC).append(" ").append(nomeVar).append(";\n");
            }
        }
    }

    private String processarTipo(NoSintatico no) {
        for (NoSintatico filho : no.getFilhos()) {
            return filho.getRotulo().toLowerCase();    
        }
        return "inteiro";
    }

    private String converterTipoC(String tipoOriginal) {
        switch (tipoOriginal) {
            case "inteiro":
                return "int";
            case "decimal":
                return "double";
            case "texto":
                return "char[]";
            default:
                return "int";
        }
    }

    private void processarLeitura(NoSintatico no) {
        String nomeVar = null;

        for (NoSintatico filho : no.getFilhos()) {
            if (filho.getRotulo().equals("ID")) {
                nomeVar = filho.getValor();
            }
        }

        if (nomeVar != null) {
            String tipoOriginal = tabelaSimbolos.get(nomeVar);
            identar();
            if (tipoOriginal != null && tipoOriginal.equals("texto")) {
                codigoC.append("fgets(").append(nomeVar)
                      .append(", sizeof(").append(nomeVar)
                      .append("), stdin);\n");
            identar();
            codigoC.append(nomeVar).append("[strcspn(")
            .append(nomeVar).append(", \"\\n\")] = 0;\n");
            } else {
                String formato = (tipoOriginal != null && tipoOriginal.equals("decimal")) 
                ? "%lf" : "%d";
                codigoC.append("scanf(\"").append(formato).append("\", &")
                .append(nomeVar).append(");\n");
            }
        }
    }

    private void processarEscrita(NoSintatico no) {
        String conteudo = null;
        boolean isString = false;

        for (NoSintatico filho : no.getFilhos()) {
            if (filho.getRotulo().equals("conteudo")) {
                for (NoSintatico filhoConteudo : filho.getFilhos()) {
                    if (filhoConteudo.getRotulo().equals("TEXTO")) {
                        conteudo = filhoConteudo.getValor();
                        isString = true;
                    } else if (filhoConteudo.getRotulo().equals("ID")) {
                        conteudo = filhoConteudo.getValor();
                        isString = false;
                    }
                }
            }
        }

        if (conteudo != null) {
            identar();
            if (isString) {
                codigoC.append("printf(\"").append(conteudo).append("\\n\");\n");
            } else {
                String tipoOriginal = tabelaSimbolos.get(conteudo);
                String formato;

                if (tipoOriginal == null) {
                    formato = "%d";
                } else {
                    switch (tipoOriginal) {
                        case "inteiro":
                            formato = "%d";
                            break;
                        case "decimal":
                            formato = "%lf";
                            break;
                        case "texto":
                            formato = "%s";
                            break;
                        default:
                            formato = "%d";
                    }
                }
                codigoC.append("printf(\"").append(formato).append("\\n\", ")
                        .append(conteudo).append(");\n");
            }
        }
    }

    private void processarAtribuicao(NoSintatico no) {
        String nomeVar = null;
        NoSintatico noExpr = null;

        for (NoSintatico filho : no.getFilhos()) {
            if (filho.getRotulo().equals("ID")) {
                nomeVar = filho.getValor();
            } else if (!filho.getRotulo().equals("ε")) {
                if (isExpressao(filho)) {
                    noExpr = filho;
                }
            }
        }

        if (nomeVar != null && noExpr != null) {
            identar();
            codigoC.append(nomeVar).append(" = ");
            StringBuilder exprSb = new StringBuilder();
            gerarExpressaoEmString(noExpr, exprSb);
            codigoC.append(exprSb.toString());
            codigoC.append(";\n");
        }
    }

    private boolean isExpressao(NoSintatico no) {
        String rotulo = no.getRotulo();
        return rotulo.equals("+") || rotulo.equals("-") || 
                rotulo.equals("*") || rotulo.equals("/") || 
                rotulo.equals("fator") || rotulo.equals("ID") || 
                rotulo.equals("NUM_DECIMAL") || rotulo.equals("NUM_INT");
    }

    private boolean isExpressaoLogicaOuCondicao(NoSintatico no) {
        String rotulo = no.getRotulo();
        return rotulo.equals("&&") || rotulo.equals("||") || 
               rotulo.equals("op_rel") || isExpressao(no);
    }

    private void gerarExpressao(NoSintatico no) {
        if (no == null) return;

        String rotulo = no.getRotulo();

        switch (rotulo) {
            case "+":
            case "-":
            case "*":
            case "/":
                if (no.getFilhos().size() >= 2) {
                    NoSintatico left = no.getFilhos().get(0);
                    NoSintatico right = no.getFilhos().get(1);

                    if (left.getRotulo().equals("fator") && isArithmeticOperatorNode(left)) {
                        codigoC.append("(");
                        gerarExpressao(left);
                        codigoC.append(")");
                    } else if (precedence(left) < precedence(no)) {
                        codigoC.append("(");
                        gerarExpressao(left);
                        codigoC.append(")");
                    } else {
                        gerarExpressao(left);
                    }

                    codigoC.append(" ").append(rotulo).append(" ");

                    if (rotulo.equals("/") && isArithmeticOperatorNode(right) && precedence(right) <= precedence(no)) {
                        // Caso especial: garantir que denominadores com expressões aditivas sejam parêntesadas
                        StringBuilder tmp = new StringBuilder();
                        gerarExpressaoEmString(right, tmp);
                        codigoC.append("(").append(tmp.toString()).append(")");
                    } else if (right.getRotulo().equals("fator") && isArithmeticOperatorNode(right)) {
                        codigoC.append("(");
                        gerarExpressao(right);
                        codigoC.append(")");
                    } else if (precedence(right) < precedence(no)) {
                        codigoC.append("(");
                        gerarExpressao(right);
                        codigoC.append(")");
                    } else {
                        gerarExpressao(right);
                    }
                }
                break;

            case "fator":
                for (NoSintatico filho : no.getFilhos()) {
                    gerarExpressao(filho);
                }
                break;

            case "NUM_DECIMAL":
            case "NUM_INT":
                codigoC.append(no.getValor());
                break;

            case "ID":
                codigoC.append(no.getValor());
                break;

            case "TEXTO":
                codigoC.append("\"").append(no.getValor()).append("\"");
                break;  
        
            default:
                for (NoSintatico filho : no.getFilhos()) {
                    gerarExpressao(filho);
                }
                break;
        }
    }

    private int precedence(NoSintatico no) {
        if (no == null) return 0;
        String r = no.getRotulo();

        // desenrolar fator que pode conter uma expressão
        if (r.equals("fator") && !no.getFilhos().isEmpty()) {
            return precedence(no.getFilhos().get(0));
        }

        switch (r) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
                return 2;
            case "NUM_DECIMAL":
            case "NUM_INT":
            case "ID":
            case "TEXTO":
                return 3;
            default:
                return 0;
        }
    }

    private void gerarExpressaoLogica(NoSintatico no) {
        if (no == null) return;

        String rotulo = no.getRotulo();

        switch (rotulo) {
            case "||":
                gerarExpressaoLogica(no.getFilhos().get(0));
                codigoC.append(" || ");
                gerarExpressaoLogica(no.getFilhos().get(1));
                break;

            case "&&":
                gerarExpressaoLogica(no.getFilhos().get(0));
                codigoC.append(" && ");
                gerarExpressaoLogica(no.getFilhos().get(1));
                break;

            case "op_rel":
                gerarExpressao(no.getFilhos().get(0));
                codigoC.append(" ").append(no.getValor()).append(" ");
                gerarExpressao(no.getFilhos().get(1));
                break;

            default:
                gerarExpressao(no);
                break;
        }
    }

    private void processarSe(NoSintatico no) {
        NoSintatico condicao = null;
        NoSintatico blocoEntao = null;
        NoSintatico noSns = null;

        for (NoSintatico filho : no.getFilhos()) {
            String rotulo = filho.getRotulo();

            if (rotulo.equals("bloco")) {
                blocoEntao = filho;
            } else if (rotulo.equals("sns")) {
                noSns = filho;
            } else if (condicao == null && isExpressaoLogicaOuCondicao(filho)) {
                condicao = filho;
            }
        }

        if (condicao != null) {
            identar();
            codigoC.append("if (");
            gerarExpressaoLogica(condicao);
            codigoC.append(") {\n");

            nivelIndentacao++;
            if (blocoEntao != null) {
                processarBloco(blocoEntao);
            }
            nivelIndentacao--;

            identar();
            codigoC.append("}");

            if (noSns != null) {
                processarSns(noSns);
            } else {
                codigoC.append("\n");
            }
        }
    }

    private void processarSns(NoSintatico no) {
        if (no.getFilhos().isEmpty()) {
            codigoC.append("\n");
            return;
        }

        NoSintatico primeiro = no.getFilhos().get(0);

        if (primeiro.getRotulo().equals("ε")) {
            codigoC.append("\n");
            return;
        }

        if (primeiro.getRotulo().equals("SENAO_SE")) {
            NoSintatico condicao = null;
            NoSintatico bloco = null;
            NoSintatico proximoSns = null;

            for (int i = 1; i < no.getFilhos().size(); i++) {
                NoSintatico filho = no.getFilhos().get(i);
                String rotulo = filho.getRotulo();

                if (condicao == null && isExpressaoLogicaOuCondicao(filho)) {
                    condicao = filho;
                } else if (rotulo.equals("bloco")) {
                    bloco = filho;
                } else if (rotulo.equals("sns")) {
                    proximoSns = filho;
                }
            }

            codigoC.append(" else if (");
            if (condicao != null) {
                gerarExpressaoLogica(condicao);
            }
            codigoC.append(") {\n");

            nivelIndentacao++;
            if (bloco != null) {
                processarBloco(bloco);
            }
            nivelIndentacao--;

            identar();
            codigoC.append("}");

            if (proximoSns != null) {
                processarSns(proximoSns);
            } else {
                codigoC.append("\n");
            }
            return;
        }

        if (primeiro.getRotulo().equals("SENAO")) {
            codigoC.append(" else {\n");

            NoSintatico bloco = null;
            for (int i = 1; i < no.getFilhos().size(); i++) {
                if (no.getFilhos().get(i).getRotulo().equals("bloco")) {
                    bloco = no.getFilhos().get(i);
                    break;
                }
            }

            nivelIndentacao++;
            if (bloco != null) {
                processarBloco(bloco);
            }
            nivelIndentacao--;

            identar();
            codigoC.append("}\n");
            return;
        }

        for (NoSintatico filho : no.getFilhos()) {
            if (filho.getRotulo().equals("sns")) {
                processarSns(filho);
            }
        }
    }

    private void processarEnquanto(NoSintatico no) {
        NoSintatico condicao = null;
        NoSintatico bloco = null;

        for (NoSintatico filho : no.getFilhos()) {
            if (condicao == null && isExpressaoLogicaOuCondicao(filho)) {
                condicao = filho;
            } else if (filho.getRotulo().equals("bloco")) {
                bloco = filho;
            }
        }

        if (condicao != null) {
            identar();
            codigoC.append("while (");
            gerarExpressaoLogica(condicao);
            codigoC.append(") {\n");

            nivelIndentacao++;
            if (bloco != null) {
                processarBloco(bloco);
            }
            nivelIndentacao--;

            identar();
            codigoC.append("}\n");
        }
    }

    private void processarPara(NoSintatico no) {
        String inicializacao = null;
        String condicao = null;
        String incremento = null;
        NoSintatico bloco = null;

        boolean encontrouIni = false;
        boolean encontrouCond = false;

        for (NoSintatico filho : no.getFilhos()) {
            if (filho.getRotulo().equals("declaracao_para") ||
                filho.getRotulo().equals("atribuicao_interna")) {
                    if (!encontrouIni) {
                        inicializacao = gerarInicializacaoPara(filho);
                        encontrouIni = true;
                    } else {
                        incremento = gerarIncrementoPara(filho);
                    }
                } else if (!encontrouCond && isExpressaoLogicaOuCondicao(filho)) {
                    condicao = gerarCondicaoPara(filho);
                    encontrouCond = true;
                } else if (filho.getRotulo().equals("bloco")) {
                    bloco = filho;
                }
            }

            if (inicializacao != null && condicao != null && incremento != null) {
                identar();
                codigoC.append("for (").append(inicializacao).append("; ")
                        .append(condicao).append("; ").append(incremento)
                        .append(") {\n");

                nivelIndentacao++;
                if (bloco != null) {
                    processarBloco(bloco);
                }
                nivelIndentacao--;

                identar();
                codigoC.append("}\n");
            }
        }

        private String gerarInicializacaoPara(NoSintatico no) {
            StringBuilder sb = new StringBuilder();

            if (no.getRotulo().equals("declaracao_para")) {
                String tipo = null;
                String nome = null;
                String valor = null;

                for (NoSintatico filho : no.getFilhos()) {
                    if (filho.getRotulo().equals("tipo")) {
                        tipo = converterTipoC(processarTipo(filho));
                    } else if (filho.getRotulo().equals("ID")) {
                        nome = filho.getValor();
                        tabelaSimbolos.put(nome, processarTipo(no.getFilhos().get(0)));
                    } else if (isExpressao(filho)) {
                        StringBuilder expr = new StringBuilder();
                        gerarExpressaoEmString(filho, expr);
                        valor = expr.toString();
                    }
                }

                sb.append(tipo).append(" ").append(nome).append(" = ").append(valor);
            } else {
                String nome = null;
                String valor = null;

                for (NoSintatico filho : no.getFilhos()) {
                    if (filho.getRotulo().equals("ID")) {
                        nome = filho.getValor();
                    } else if (isExpressao(filho)) {
                        StringBuilder expr = new StringBuilder();
                        gerarExpressaoEmString(filho, expr);
                        valor = expr.toString();
                    }
                }

                sb.append(nome).append(" = ").append(valor);
            }

            return sb.toString();
        }

        private String gerarCondicaoPara(NoSintatico no) {
            StringBuilder sb = new StringBuilder();
            gerarExpressaoLogicaEmString(no, sb);
            return sb.toString();
        }

        private String gerarIncrementoPara(NoSintatico no) {
            StringBuilder sb = new StringBuilder();
            
            String nome = null;
            String valor = null;

            for (NoSintatico filho : no.getFilhos()) {
                if (filho.getRotulo().equals("ID")) {
                    nome = filho.getValor();
                } else if (isExpressao(filho)) {
                    StringBuilder expr = new StringBuilder();
                    gerarExpressaoEmString(filho, expr);
                    valor = expr.toString();
                }
            }

            sb.append(nome).append(" = ").append(valor);
            return sb.toString();
        }

        private void gerarExpressaoEmString(NoSintatico no, StringBuilder sb) {
            if (no == null) return;

            String rotulo = no.getRotulo();

            switch (rotulo) {
                case "+":
                case "-":
                case "*":
                case "/":
                    if (no.getFilhos().size() >= 2) {
                        NoSintatico left = no.getFilhos().get(0);
                        NoSintatico right = no.getFilhos().get(1);

                        if (left.getRotulo().equals("fator") && isArithmeticOperatorNode(left)) {
                            sb.append("(");
                            gerarExpressaoEmString(left, sb);
                            sb.append(")");
                        } else if (precedence(left) < precedence(no)) {
                            sb.append("(");
                            gerarExpressaoEmString(left, sb);
                            sb.append(")");
                        } else {
                            gerarExpressaoEmString(left, sb);
                        }

                        sb.append(" ").append(rotulo).append(" ");

                        if (rotulo.equals("/") && isArithmeticOperatorNode(right) && precedence(right) <= precedence(no)) {
                            StringBuilder tmp = new StringBuilder();
                            gerarExpressaoEmString(right, tmp);
                            sb.append("(").append(tmp.toString()).append(")");
                        } else if (right.getRotulo().equals("fator") && isArithmeticOperatorNode(right)) {
                            sb.append("(");
                            gerarExpressaoEmString(right, sb);
                            sb.append(")");
                        } else if (precedence(right) < precedence(no)) {
                            sb.append("(");
                            gerarExpressaoEmString(right, sb);
                            sb.append(")");
                        } else {
                            gerarExpressaoEmString(right, sb);
                        }
                    }
                    break;

                case "fator":
                    for (NoSintatico filho : no.getFilhos()) {
                        gerarExpressaoEmString(filho, sb);
                    }
                    break;

                case "NUM_DECIMAL":
                case "NUM_INT":
                    sb.append(no.getValor());
                    break;

                case "ID":
                    sb.append(no.getValor());
                    break;

                default:
                    for (NoSintatico filho : no.getFilhos()) {
                        gerarExpressaoEmString(filho, sb);
                    }
                    break;
            }
        }

        private void gerarExpressaoLogicaEmString(NoSintatico no, StringBuilder sb) {
            if (no == null) return;

            String rotulo = no.getRotulo();

            switch (rotulo) {
                case "||":
                case "&&":
                    gerarExpressaoLogicaEmString(no.getFilhos().get(0), sb);
                    sb.append(" ").append(rotulo).append(" ");
                    gerarExpressaoLogicaEmString(no.getFilhos().get(1), sb);
                    break;


                case "op_rel":
                    gerarExpressaoEmString(no.getFilhos().get(0), sb);
                    sb.append(" ").append(no.getValor()).append(" ");
                    gerarExpressaoEmString(no.getFilhos().get(1), sb);
                    break;

                default:
                    gerarExpressaoEmString(no, sb);
                    break;
            }
        }

        public void salvarArquivo(String codigo, String nomeArquivo) throws IOException {
            FileWriter fw = new FileWriter(nomeArquivo);
            fw.write(codigo);
            fw.close();
            System.out.println("Código C gerado e salvo em: " + nomeArquivo);     
        }
    
    private boolean isArithmeticOperatorNode(NoSintatico no) {
        if (no == null) return false;
        String r = no.getRotulo();
        if (r.equals("+") || r.equals("-") || r.equals("*") || r.equals("/")) return true;
        if (r.equals("fator") && !no.getFilhos().isEmpty()) {
            return isArithmeticOperatorNode(no.getFilhos().get(0));
        }
        return false;
    }
}