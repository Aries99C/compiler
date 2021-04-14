package lexical;

import java.util.List;

public class Lexer {

    private List<Character> characters;

    public Lexer(String filename) {
        CharacterReader characterReader = new CharacterReader(filename);
        this.characters = characterReader.getBuffer();
    }

    public void parse() {

    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/lexical/test.c");
    }
}
