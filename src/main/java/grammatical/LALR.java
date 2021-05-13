package grammatical;

import lexical.ErrorInfo;
import lexical.Token;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import semantic.Symbol;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

public class LALR {

    public final List<StateEntry> stateEntries = new ArrayList<>();
    public final Map<String, Integer> symbolMap = new HashMap<>();
    public final List<Production> productions = new ArrayList<>();
    public final List<Symbol> symbols = new ArrayList<>();
    public final List<ErrorInfo> errorInfos = new ArrayList<>();


    private void getStates() {
        // open LALR.xml
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse("src/main/java/grammatical/LALR.xml");

            // read tree nodes of states
            NodeList stateList = document.getElementsByTagName("LALRState");
            for (int i = 0; i < stateList.getLength(); i++) {
                Element state = (Element) stateList.item(i);
                StateEntry stateEntry = new StateEntry();

                String attribute = state.getAttribute("Index");
                stateEntry.stateIndex = Integer.parseInt(attribute);

                // read action according to symbol and state
                NodeList children = state.getChildNodes();
                List<TableEntry> tableEntries = new ArrayList<>();
                for (int j = 0; j <children.getLength(); j++) {
                    if (children.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element table = (Element) children.item(j);
                        TableEntry tableEntry = new TableEntry();

                        String value = table.getAttribute("Value");
                        tableEntry.value = Integer.parseInt(value);
                        String symbolIndex = table.getAttribute("SymbolIndex");
                        tableEntry.symbolIndex = Integer.parseInt(symbolIndex);
                        String action = table.getAttribute("Action");
                        tableEntry.action = Integer.parseInt(action);

                        tableEntries.add(tableEntry);
                    }
                }
                stateEntry.entries = tableEntries;
                stateEntries.add(stateEntry);
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    private void getSymbolMap() {
        // open LALR.xml
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse("src/main/java/grammatical/LALR.xml");

            // read terminal symbols
            NodeList symbols = document.getElementsByTagName("Symbol");
            for (int i = 0; i < symbols.getLength(); i++)
            {
                Element symbol = (Element) symbols.item(i);

                String name = symbol.getAttribute("Name");
                String index = symbol.getAttribute("Index");
                symbolMap.put(name, Integer.parseInt(index));
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private void getProductions() {
        try {
            File file = new File("src/main/java/grammatical/productions.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            Production production;
            while ((line=reader.readLine())!=null) {
                String left = line.split("::=")[0].trim();
                String right = line.split("::=")[1].trim();
                production = new Production(left, right.split("\\s+"));
                productions.add(production);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeProductions() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/grammatical/grammar.txt"))) {
            for (Production production : productions) {
                StringBuilder builder = new StringBuilder();
                builder.append("<").append(production.left).append("> ::= ");
                for (String right : production.rights) {
                    boolean find = false;
                    for (Production findLeft : productions) {
                        if (findLeft.left.equals(right)) {
                            find = true;
                            break;
                        }
                    }
                    if (find) {
                        builder.append("<").append(right).append("> ");
                    } else {
                        if (!right.equals("epsilon")) {
                            builder.append(right);
                        }
                        builder.append(" ");
                    }
                }
                builder.append("\n");
                writer.write(builder.toString());
            }
            writer.write("\"Start Symbol\" = <P>");
        } catch (Exception ignored) {}
    }

    public LALR() {
        getStates();
        getSymbolMap();
        getProductions();
        writeProductions();
    }

    public void addSymbol(String id, int line) {
        boolean find = false;
        for (Symbol symbol : symbols) {
            if (symbol.name.equals(id)) {
                errorInfos.add(new ErrorInfo("duplicate declaration " + id, line));
                find = true;
                break;
            }
        }
        if (!find) {
            symbols.add(new Symbol(id));
        }
    }

    public TreeNode parse(List<Token> tokens) {
        // token stack
        Stack<TreeNode> symbolStack = new Stack<>();
        // state stack
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(0);
        // node list
        List<TreeNode> nodes = new ArrayList<>();


        int state = 0;
        int j = 0;
        int line = 0;
        // keep reading from buffer of token list
        Token token = tokens.get(j);
        while (token != null) {
            line = token.line;
            state = stateStack.peek();
            StateEntry stateEntry = stateEntries.get(state);

            int symbolIndex = symbolMap.get(token.info[0]);
            TreeNode node = new TreeNode(symbolIndex, token, true);
            nodes.add(node);
//            System.out.println("state: " + state + ", token: " + token.info[0]);

            /* find action according to state and token */
            // record find action flag
            boolean error = true;
            for (TableEntry tableEntry : stateEntry.entries) {
                if (tableEntry.symbolIndex == symbolIndex) {
                    error = false;
                    int action = tableEntry.action;
                    /* shift action */
                    if (action == 1) {
                        // semantic

                        // syntax
                        state = tableEntry.value;
                        symbolStack.push(node);
                        stateStack.push(state);
                        j++;
//                        System.out.println("shift to state: " + state);
                    }
                    /* reduce action */
                    else if (action == 2) {
                        int productionIndex = tableEntry.value;
                        String left = productions.get(productionIndex).left;
                        String[] rights = productions.get(productionIndex).rights;
//                        System.out.println("reduce by production: " + productionIndex);

                        /* syntax */
                        if (!symbolMap.containsKey(left)) {
                            break;
                        }
                        int leftIndex = symbolMap.get(left);
                        // pop elements from token stack and state stack
                        assert token != null;
                        TreeNode parent = new TreeNode(leftIndex, new Token(left, new String[]{left, "_"}, token.line), false);
                        for (String right : rights) {
                            if (!right.equals("epsilon")) {
                                TreeNode child = symbolStack.pop();
                                stateStack.pop();
                                // link between parent and children
                                child.parent = parent;
                                parent.children.add(child);
                            }
                        }
                        Collections.reverse(parent.children);

                        // semantic
                        // empty production

                        // non-empty production

                        // push parent
                        symbolStack.push(parent);
                        nodes.add(parent);

                        // goto action
                        state = stateStack.peek();
                        StateEntry gotoState = stateEntries.get(state);
                        for (TableEntry gotoTable : gotoState.entries) {
                            if (gotoTable.symbolIndex == leftIndex && gotoTable.action == 3) {
                                state = gotoTable.value;
                                stateStack.push(state);
//                                System.out.println("goto: " + state);
                                break;
                            }
                        }
                    }
                    /* acc action */
                    else if (action == 4) {
                        return nodes.get(nodes.size()-2);
                    }
                }
                if (j < tokens.size()) {
                    token = tokens.get(j);
                } else {
                    token = null;
                }
            }
            // no action
            if (error) {
                break;
            }
        }
        assert token != null;
        return new TreeNode(1, new Token("error", new String[]{"Syntax error at Line [" + line + "]: ", "[" + state + ", " + token.info[0] + "]"}, line), true);
    }
}
