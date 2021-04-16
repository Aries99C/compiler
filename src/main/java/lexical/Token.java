package lexical;

public class Token {

    // lexical string
    public String str;
    // species code and pointer
    public String[] info;

    public Token(String str, String[] info) {
        this.str = str;
        this.info = info;
    }
}
