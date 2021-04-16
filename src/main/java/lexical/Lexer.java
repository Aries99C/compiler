package lexical;

import java.util.List;
import java.util.Map;

public class Lexer {

    private final char[] input;     // input context
    private List<Token> tokens;     // save tokens
    private List<ErrorInfo> errors; // save errors

    public Lexer(String filename) {
        CharacterReader characterReader = new CharacterReader(filename);
        List<Character> buffer = characterReader.getBuffer();
        input = new char[buffer.size()];
        int index = 0;
        for (char c : buffer) {
            input[index] = c;
            index++;
        }
    }

    public void parse() {
        /* run dfa */
        DFA dfa = new DFA();
        dfa.run(input);
        /* output */
        this.tokens = dfa.getTokens();
        this.errors = dfa.getErrors();
        System.out.println("****************************************");
        System.out.println("Input:");
        for (int i = 0; i < input.length-1; i++) {
            System.out.print(input[i]);
        }
        System.out.println();
        System.out.println("****************************************");
        System.out.println("Token:");
        for (Token token : tokens) {
            System.out.println(String.format("%-10s", token.str) + "\t" + "< " + String.format("%-6s", token.info[0])  + "\t, " + String.format("%-6s", token.info[1]) + " >");
        }
        System.out.println("****************************************");
        System.out.println("Error:");
        for (ErrorInfo error : errors) {
            System.out.println("Lexical error at line [" + error.info + "] -> [ " + error.line + " ].");
        }
        System.out.println("****************************************");
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<ErrorInfo> getErrors() {
        return errors;
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/main/java/lexical/test.c");
        lexer.parse();
    }
}
