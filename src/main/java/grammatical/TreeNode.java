package grammatical;

import lexical.Token;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {

    public int index;
    public Token token;
    public List<TreeNode> children;
    public TreeNode parent;

    public TreeNode(int index, Token token) {
        this.index = index;
        this.token = token;
        this.children = new ArrayList<>();
    }
}
