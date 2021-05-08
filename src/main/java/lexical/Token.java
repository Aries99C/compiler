package lexical;

public class Token {

    // lexical string
    public String str;
    // species code and pointer
    public String[] info;
    // line
    public int line;

    public Token(String str, String[] info, int line) {
        this.str = str;
        this.info = info;
        this.line = line;
    }
}
