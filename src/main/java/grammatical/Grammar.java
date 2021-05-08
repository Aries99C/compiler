package grammatical;

import lexical.Lexer;
import lexical.Token;

import java.util.ArrayList;
import java.util.List;

public class Grammar {

    public static void updateTree(TreeNode root) {
        if (root.parent != null) {
            int minLine = root.token.line;
            for (TreeNode brother : root.parent.children) {
                int brotherLine = brother.token.line;
                if (brotherLine < minLine) {
                    minLine = brotherLine;
                }
            }
            root.token.line = minLine;
        }
        for (TreeNode child : root.children) {
            updateTree(child);
        }
    }

    public static void showTree(TreeNode root, int depth) {
        String output = root.token.info[0];
        if (!root.token.info[1].equals("_")) {
            output = output + ":" + root.token.info[1];
        }
        output = output + "(" + root.token.line + ")";
        System.out.println(output);
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }
        for (TreeNode child : root.children) {
            showTree(child, depth+1);
        }
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/main/java/lexical/test.c");
        lexer.parse();
        List<Token> tokens = lexer.getTokens();
        tokens.add(new Token("$", new String[]{"EOF", "$"}, 0));
//        List<Token> tokens = new ArrayList<>();
//        tokens.add(new Token("int", new String[]{"INT", "_"}, 1));
//        tokens.add(new Token("a", new String[]{"ID", "a"}, 1));
//        tokens.add(new Token("=", new String[]{"EQS", "_"}, 1));
//        tokens.add(new Token("3", new String[]{"DINT", "3"}, 1));
//        tokens.add(new Token(";", new String[]{"SEMI", "_"}, 1));
//        tokens.add(new Token("$", new String[]{"EOF", "$"}, 0));
        LALR lalr = new LALR();
        TreeNode root = lalr.parse(tokens);
        updateTree(root);
        showTree(root, 1);
    }
}
