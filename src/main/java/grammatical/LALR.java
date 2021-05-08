package grammatical;

import lexical.Token;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class LALR {

    public final List<StateEntry> stateEntries = new ArrayList<>();
    public final Map<String, Integer> symbolMap = new HashMap<>();
    public final List<Production> productions = new ArrayList<>();

    private void getStates() {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse("src/main/java/grammatical/LALR.xml");

            NodeList stateList = document.getElementsByTagName("LALRState");
            for (int i = 0; i < stateList.getLength(); i++) {
                Element state = (Element) stateList.item(i);
                StateEntry stateEntry = new StateEntry();

                String attribute = state.getAttribute("Index");
                stateEntry.stateIndex = Integer.parseInt(attribute);

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
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse("src/main/java/grammatical/LALR.xml");

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
                production = new Production(left, right.split(" "));
                productions.add(production);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LALR() {
        getStates();
        getSymbolMap();
        getProductions();
    }

    public TreeNode parse(List<Token> tokens) {
        Stack<TreeNode> tokenStack = new Stack<>();
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(0);
        List<TreeNode> nodes = new ArrayList<>();
        int state;
        int j = 0;
        Token token = tokens.get(j);
        while (token != null) {
            int count = 0;
            state = stateStack.peek();
            StateEntry stateEntry = stateEntries.get(state);

            int symbolIndex = symbolMap.get(token.info[0]);
            TreeNode node = new TreeNode(symbolIndex, token);
            nodes.add(node);
            System.out.println("state: " + state + " read: " + token.info[0]);

            for (TableEntry tableEntry : stateEntry.entries) {
                if (tableEntry.symbolIndex == symbolIndex) {
                    count = -100;
                    int action = tableEntry.action;
                    if (action == 1) {
                        state = tableEntry.value;
                        tokenStack.push(node);
                        stateStack.push(state);
                        j++;
                    }
                    else if (action == 2) {
                        int productionIndex = tableEntry.value;
                        String left = productions.get(productionIndex).left;
                        if (!symbolMap.containsKey(left)) {
                                break;
                        }
                        int leftIndex = symbolMap.get(left);
                        System.out.println("action: " + tableEntry.action + " value: " + tableEntry.value);
                        TreeNode parent = new TreeNode(leftIndex, new Token(left, new String[]{left, "_"}, token.line));
                        for (int i = 0; i < productions.get(productionIndex).rights.length; i++) {
                            TreeNode child = tokenStack.pop();
                            stateStack.pop();
                            child.parent = parent;
                            parent.children.add(child);
                        }
                        Collections.reverse(parent.children);
                        tokenStack.push(parent);
                        nodes.add(parent);

                        state = stateStack.peek();
                        StateEntry gotoState = stateEntries.get(state);
                        for (TableEntry gotoTable : gotoState.entries) {
                            if (gotoTable.symbolIndex == leftIndex && gotoTable.action == 3) {
                                state = gotoTable.value;
                                System.out.println("action: " + gotoTable.action + " value: " + tableEntry.value);
                                stateStack.push(state);
                                break;
                            }
                        }
                    }
                    else if (action == 4) {
                        return nodes.get(nodes.size()-2);
                    }
                    count++;
                }
                if (j < tokens.size()) {
                    token = tokens.get(j);
                } else {
                    token = null;
                }
            }
        }
        return null;
    }
}
