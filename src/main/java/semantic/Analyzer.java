package semantic;

import grammatical.LALR;
import grammatical.TreeNode;
import lexical.ErrorInfo;
import lexical.Lexer;
import lexical.Token;

import java.util.List;

public class Analyzer {

    public void showSymbols(List<TreeNode> symbols) {
        for (TreeNode symbol : symbols) {
            String builder = "id: " + symbol.token.str + " " +
                    "type: " + symbol.attribute.get("type") + " " +
                    "line: " + symbol.token.line + " " +
                    "offset: " + symbol.attribute.get("offset") + " ";
            if (symbol.attribute.get("type").equals("proc")) {
                builder = builder + "types: " + symbol.attribute.get("types") + " ";
            }
            if (symbol.attribute.get("type").equals("record")) {
                builder = builder + "params: " + symbol.attribute.get("params") + " ";
            }
            System.out.println(builder);
        }
    }

    public void showErrors(List<ErrorInfo> errorInfos) {
        for (ErrorInfo errorInfo : errorInfos) {
            System.out.println(errorInfo.info);
        }
    }

    public void showCodes(List<String> add3Code, List<String> tuple4Code) {
        for (int i = 0; i < add3Code.size(); i++) {
            String code4 = String.format("%-25s", tuple4Code.get(i));
            String code3 = String.format("%-25s", add3Code.get(i));
            String line = i + "\t" + code4 + code3 ;
            System.out.println(line);
        }
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/main/java/lexical/test.c");
        lexer.parse();
        List<Token> tokens = lexer.getTokens();
        tokens.add(new Token("$", new String[]{"EOF", "$"}, 0));
        LALR lalr = new LALR();
        lalr.parse(tokens);
        Analyzer analyzer = new Analyzer();
        List<ErrorInfo> errorInfos = lalr.semanticErrors;
        List<TreeNode> symbols = lalr.symbols;
        List<String> add3Code = lalr.add3Code;
        List<String> tuple4Code = lalr.tuple4Code;
        System.out.println("******************************");
        System.out.println("Symbols:");
        analyzer.showSymbols(symbols);
        System.out.println("******************************");
        System.out.println("Semantic errors:");
        analyzer.showErrors(errorInfos);
        System.out.println("******************************");
        System.out.println("Generated codes:");
        analyzer.showCodes(add3Code, tuple4Code);
        System.out.println("******************************");
    }
}
