package grammatical;

import lexical.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeNode {

    public int index;
    public Token token;
    public List<TreeNode> children;
    public TreeNode parent;
    public boolean terminal;
    public Map<String, String> attribute;

    public TreeNode(int index, Token token, boolean terminal) {
        this.index = index;
        this.token = token;
        this.children = new ArrayList<>();
        this.terminal = terminal;
        this.attribute = new HashMap<>();
    }
}
