/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Usuario
 */

import lexico.*;
import sintatico.*;
import gerador.*;
import semantico.*;
import java.io.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {

            System.out.println("Selecione o arquivo de código-fonte (.doth) para compilar:");
            System.out.println("1. teste_completo");
            System.out.println("2. teste_sintatico");
            System.out.println("3. teste_semantico");
            String opcao = System.console().readLine();

            String nomeArquivo = switch (opcao) {
                case "1" -> "teste.txt";
                case "2" -> "testesintatico.txt";
                case "3" -> "testesemantico.txt";
                default -> {
                    System.out.println("Opção inválida.");
                    yield null;
                }
            };

            if (nomeArquivo == null) {
                return;
            }

            System.out.println("=== COMPILADOR ===");
            System.out.println("Arquivo: " + nomeArquivo + "\n");

            // Análise léxica
            System.out.println("Executando analise lexica...");
            Lexer lexico = new Lexer(nomeArquivo);
            List<Token> tokens = lexico.analisar();
            
            System.out.println("Deseja imprimir os tokens? (s/n)");
            String resposta = System.console().readLine();
            if (resposta != null && resposta.toLowerCase().equals("s")) {
                // Imprime os tokens
                lexico.imprimirTokens();
            }

            // Verifica erros
            if (lexico.temErros()) {
                System.out.println("Erros léxicos encontrados:");
                for (String erro : lexico.getErros()) {
                    System.out.println("  - " + erro);
                }
                return;
            }
            System.out.println("\n Análise léxica concluída com sucesso!");
            
            // Análise sintática
            System.out.println("\nExecutando análise sintática...");
            Parser sintatico = new Parser(tokens);
            NoSintatico ast = sintatico.analisar();
            
            System.out.println("Deseja imprimir a árvore sintática? (s/n)");
            String respostaArv = System.console().readLine();
            if (respostaArv != null && respostaArv.toLowerCase().equals("s")) {
                sintatico.imprimirArvore();
            }
            
            if (sintatico.temErros()) {
                System.out.println("Erros sintáticos encontrados:");
                for (String erro : sintatico.getErros()) {
                    System.out.println("  - " + erro);
                }
                return;
            }

            // Analise semantica
            System.out.println("\nExecutando analise semantica...");
            AnalisadorSemantico semantico = new AnalisadorSemantico();
            semantico.analisar(ast);

            if (semantico.temErros()) {
                System.out.println("Erros semanticos encontrados:");
                for (String erro : semantico.getErros()) {
                    System.out.println("  - " + erro);
                }
                return;
            }
            System.out.println("\n Analise semantica concluida com sucesso!");

            // Geração de código C
            System.out.println("\nGerando código C...");
            GeradorCodigoC gerador = new GeradorCodigoC();
            String codigoC = gerador.gerarCodigo(ast);

            gerador.salvarArquivo(codigoC, "teste.c");

            System.out.println("\nCódigo C gerado e salvo em teste.c com sucesso!");
            System.out.println(codigoC);

        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
