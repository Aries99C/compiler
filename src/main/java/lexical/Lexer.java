package lexical;

import java.util.List;
import java.util.Map;

public class Lexer {

    private final char[] input;             // input context
    private Map<String, String[]> tokens;   // save tokens
    private Map<String, Integer> errors;    // save errors

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
        System.out.println("****************************************");
        System.out.println("Token:");
        for (Map.Entry<String, String[]> token : tokens.entrySet()) {
            System.out.println(token.getKey() + "\t" + "< " + token.getValue()[0] + "\t, " + token.getValue()[1] + " >");
        }
        System.out.println("****************************************");
        System.out.println("Error:");
        for (Map.Entry<String, Integer> error : errors.entrySet()) {
            System.out.println("Lexical error at line [" + error.getValue() + "]: [ " + error.getKey() + " ].");
        }
        System.out.println("****************************************");
    }

    public Map<String, String[]> getTokens() {
        return tokens;
    }

    public Map<String, Integer> getErrors() {
        return errors;
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/main/java/lexical/test.c");
        lexer.parse();
    }
}
