package grammatical;

import lexical.Lexer;
import lexical.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Grammar {

    public static void updateTree(TreeNode root) {
        root.token.line = getLine(root);
        for (TreeNode child : root.children) {
            updateTree(child);
        }
    }

    public static int getLine(TreeNode root) {
        if (!root.terminal) {
            if (root.children.size() > 0) {
                root.token.line = getLine(root.children.get(0));
                return root.token.line;
            }
        }
        return root.token.line;
    }

    public static void showTree(TreeNode root, int depth) {
        if (root.token.info[0].contains("semantic")) {
            return;
        }
        StringBuffer output = new StringBuffer();
        output.append("  ".repeat(Math.max(0, depth)));
        output.append(root.token.info[0]);
        if (root.token.info[0].contains("Syntax error")) {
            output.append(root.token.info[1]);
            System.out.println(output);
        } else {
            if (!root.token.info[1].equals("_")) {
                output.append(":").append(root.token.info[1]);
            }
            output.append("(").append(root.token.line).append(")");
            for (Map.Entry<String, String> entry : root.attribute.entrySet()) {
                output.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
            }
            System.out.println(output);
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
        LALR lalr = new LALR();
        TreeNode root = lalr.parse(tokens);
        updateTree(root);
        showTree(root, 0);
    }
}
