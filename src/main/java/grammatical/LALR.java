package grammatical;

import lexical.ErrorInfo;
import lexical.Token;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

public class LALR {

    public final List<StateEntry> stateEntries = new ArrayList<>();
    public final Map<String, Integer> symbolMap = new HashMap<>();
    public final List<Production> productions = new ArrayList<>();
    public final List<TreeNode> symbols = new ArrayList<>();
    public final List<ErrorInfo> semanticErrors = new ArrayList<>();
    public final List<String> add3Code = new ArrayList<>();
    public final List<String> tuple4Code = new ArrayList<>();
    private int tmpNum = 0;
    private String t;
    private String w;
    private String offset = "0";

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
                        if (symbolStack.size() > 0 && symbolStack.peek().token.info[0].equals("S")) {
                            if (symbolStack.peek().attribute.get("nextList") != null) {
                                backPatch(turnList(symbolStack.peek().attribute.get("nextList")), nextQuad());
                            }
                        }
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
                        switch (productionIndex) {
                            // declaration
                            case 11: {
                                // X::=int {X.type=INT; X.width=4;}
                                parent.attribute.put("type", "int");
                                parent.attribute.put("width", "4");
                                break;
                            } case 12: {
                                // X::=float {X.type=FLOAT; X.width=8;}
                                parent.attribute.put("type", "float");
                                parent.attribute.put("width", "8");
                                break;
                            } case 13: {
                                // X::=char {X.type=CHAR; X.width=1;}
                                parent.attribute.put("type", "char");
                                parent.attribute.put("width", "1");
                                break;
                            } case 15: {
                                // C::= {C.type=t; C.width=w;}
                                parent.attribute.put("type", t);
                                parent.attribute.put("width", w);
                                break;
                            } case 14: {
                                // C::=[ DINT ] C1 {C.type=array(DINT.value, C1.type); C.width = DINT.val * C1.width;}
                                parent.attribute.put("type", "array(" + parent.children.get(1).token.str + ", " + parent.children.get(3).attribute.get("type") + ")");
                                int width = Integer.parseInt(parent.children.get(1).token.str) * Integer.parseInt(parent.children.get(3).attribute.get("width"));
                                parent.attribute.put("width", String.valueOf(width));
                                break;
                            } case 10: {
                                // TM::= {t=X.type; w=X.width;}
                                t = symbolStack.peek().attribute.get("type");
                                w = symbolStack.peek().attribute.get("width");
                                break;
                            } case 9: {
                                // T::=* T1 {T.type=pointer(T1.type); T.width=4;}
                                parent.attribute.put("type", "pointer(" + parent.children.get(1).attribute.get("type") + ")");
                                parent.attribute.put("width", "4");
                                break;
                            } case 8: {
                                // T::=X TM C {T.type=C.type, T.width=C.width}
                                parent.attribute.put("type", parent.children.get(2).attribute.get("type"));
                                parent.attribute.put("width", parent.children.get(2).attribute.get("width"));
                                break;
                            } case 3: {
                                // D::=T IDN SEMI {enter(IDN.lexeme, T.type, offset); offset=offset+T.width; D.params=IDN.lexeme;}
                                if (enter(parent.children.get(1).token.str, parent.children.get(0).attribute.get("type"), offset, parent.children.get(1).token.line)) {
                                    int newOffset = Integer.parseInt(offset) + Integer.parseInt(parent.children.get(0).attribute.get("width"));
                                    offset = String.valueOf(newOffset);
                                    parent.attribute.put("params", parent.children.get(1).token.str);
                                }
                                break;
                            } case 6: {
                                // DM1::= {enter(IDN.lexeme, RECORD, offset);}
                                enter(symbolStack.peek().token.str, "record", offset, symbolStack.peek().token.line);
                                break;
                            } case 7: {
                                // DM2::= {enter(IDN.lexeme, PROC, offset);}
                                enter(symbolStack.peek().token.str, "proc", offset, symbolStack.peek().token.line);
                                break;
                            } case 16: {
                                // M::=M1 , X IDN {M.size=M1.size+1; M.types=M1.types+X.type; enter(IDN.lexeme, X.type, offset); offset=offset+X.width; M.params=M1.params+IDN.lexeme;}
                                int newSize = Integer.parseInt(parent.children.get(0).attribute.get("size")) + 1;
                                parent.attribute.put("size", String.valueOf(newSize));
                                parent.attribute.put("types", parent.children.get(0).attribute.get("types") + ";" + parent.children.get(2).attribute.get("type"));
                                if (enter(parent.children.get(3).token.str, parent.children.get(2).attribute.get("type"), offset, parent.children.get(3).token.line)) {
                                    int newOffset = Integer.parseInt(offset) + Integer.parseInt(parent.children.get(2).attribute.get("width"));
                                    offset = String.valueOf(newOffset);
                                    parent.attribute.put("params", parent.children.get(0).attribute.get("params")+ ";" +parent.children.get(3).token.str);
                                }
                                break;
                            } case 17: {
                                // M::=X IDN {M.size=1; M.types=X.type; enter(IDN.lexeme, X.type, offset); offset=offset+X.width; M.params=IDN.lexeme;}
                                parent.attribute.put("size", "1");
                                parent.attribute.put("types", parent.children.get(0).attribute.get("type"));
                                if (enter(parent.children.get(1).token.str, parent.children.get(0).attribute.get("type"), offset, parent.children.get(1).token.line)) {
                                    int newOffset = Integer.parseInt(offset) + Integer.parseInt(parent.children.get(0).attribute.get("width"));
                                    offset = String.valueOf(newOffset);
                                    parent.attribute.put("params", parent.children.get(1).token.str);
                                }
                                break;
                            } case 5: {
                                // D::=PROC X IDN DM2 ( M ) { P } {IDN.lexeme.size=M.size; IDN.lexeme.types=M.types; D.params=IDN.lexeme}
                                TreeNode symbol = lookUp(parent.children.get(2).token.str);
                                if (symbol != null && symbol.attribute.get("type").equals("proc")) {
                                    symbol.attribute.put("size", parent.children.get(5).attribute.get("size"));
                                    symbol.attribute.put("types", parent.children.get(5).attribute.get("types"));
                                    parent.attribute.put("params", symbol.token.str);
                                }
                                break;
                            } case 4: {
                                // D::=STRUCT IDN DM1 { P } {IDN.lexeme.params=P.params; D.params=IDN.lexeme;}
                                TreeNode symbol = lookUp(parent.children.get(1).token.str);
                                if (symbol != null && symbol.attribute.get("type").equals("record")) {
                                    symbol.attribute.put("params", parent.children.get(4).attribute.get("params"));
                                    parent.attribute.put("params", symbol.token.str);
                                }
                                break;
                            } case 0: {
                                // P::=D P1 {P.params=D.params+P1.params}
                                String params = parent.children.get(1).attribute.get("params");
                                if (params == null) {
                                    parent.attribute.put("params", parent.children.get(0).attribute.get("params"));
                                } else {
                                    parent.attribute.put("params", parent.children.get(0).attribute.get("params") + ";" + params);
                                }
                                break;
                            } case 1: {
                                // P::=S P1 {P.params=P1.params}
                                String params = parent.children.get(1).attribute.get("params");
                                if (params != null) {
                                    parent.attribute.put("params", params);
                                }
                                break;
                            }
                            // assign
                            case 35: {
                                // F::=DINT {F.addr=DINT.val; F.type=int;}
                                parent.attribute.put("addr", parent.children.get(0).token.str);
                                parent.attribute.put("type", "int");
                                break;
                            } case 37: {
                                // F::=IDN {addr=lookUp(IDN.lexeme); if addr==null then error; else F.addr=addr; F.type=IDN.lexeme.type}
                                TreeNode symbol = lookUp(parent.children.get(0).token.str);
                                if (symbol != null) {
                                    parent.attribute.put("addr", symbol.token.str);
                                    parent.attribute.put("type", symbol.attribute.get("type"));
                                } else {
                                    semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: undefined symbol " + parent.children.get(0).token.str, parent.children.get(0).token.line));
                                }
                                break;
                            } case 38: {
                                // F::=FP {F.addr=FP.val; F.type=float;}
                                parent.attribute.put("addr", parent.children.get(0).token.str);
                                parent.attribute.put("type", "float");
                                break;
                            } case 39: {
                                // F::=CH {F.addr=CH.val; F.type=char;}
                                parent.attribute.put("addr", parent.children.get(0).token.str);
                                parent.attribute.put("type", "char");
                                break;
                            } case 40: {
                                // F::=L {F.addr=L.array[L.offset]; F.type=L.type}
                                parent.attribute.put("addr", parent.children.get(0).attribute.get("array") + "[" + parent.children.get(0).attribute.get("offset") + "]");
                                parent.attribute.put("type", parent.children.get(0).attribute.get("type"));
                                break;
                            } case 36: {
                                // F::=IDN . F1 {addr=lookUp(IDN.lexeme); if addr==null then error; if !IDN.F1 then error; else F.addr=IDN.lexeme.F1.addr; F.type=F1.type;}
                                TreeNode symbol = lookUp(parent.children.get(0).token.str);
                                if (symbol != null) {
                                    if (parent.children.get(2).attribute.get("addr") == null) {
                                        break;
                                    }
                                    String[] params = symbol.attribute.get("params").split(";");
                                    boolean find = false;
                                    for (String param : params) {
                                        if (param.equals(parent.children.get(2).attribute.get("addr"))) {
                                            find = true;
                                            break;
                                        }
                                    }
                                    if (!find) {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: record " + parent.children.get(0).token.str + " do not have variable " + parent.children.get(2).attribute.get("addr"), parent.children.get(0).token.line));
                                    } else {
                                        parent.attribute.put("addr", parent.children.get(2).attribute.get("addr"));
                                        parent.attribute.put("type", parent.children.get(2).attribute.get("type"));
                                    }
                                } else {
                                    semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: undefined symbol " + parent.children.get(0).token.str, parent.children.get(0).token.line));
                                }
                                break;
                            } case 34: {
                                // F::=( E ) {F.addr=E.addr; F.type=E.type;}
                                parent.attribute.put("addr", parent.children.get(1).attribute.get("addr"));
                                parent.attribute.put("type", parent.children.get(1).attribute.get("type"));
                                break;
                            } case 33: case 31: {
                                // G::=F {G.addr=F.addr; G.type=F.type;}
                                // E::=G {E.addr=G.addr; E.type=G.type;}
                                parent.attribute.put("addr", parent.children.get(0).attribute.get("addr"));
                                parent.attribute.put("type", parent.children.get(0).attribute.get("type"));
                                break;
                            } case 32: {
                                // G::=G1 * F {if F.type!=G1.type then error; else G.addr=newTemp(); G.type=G1.type; gen(G.addr=G1.addr*F.addr);}
                                if (parent.children.get(0).attribute.get("addr") == null || parent.children.get(2).attribute.get("addr") == null) {
                                    break;
                                } else {
                                    if (parent.children.get(0).attribute.get("type").equals(parent.children.get(2).attribute.get("type"))) {
                                        parent.attribute.put("addr", newTemp());
                                        parent.attribute.put("type", parent.children.get(0).attribute.get("type"));
                                        add3Code.add(parent.attribute.get("addr") + "=" + parent.children.get(0).attribute.get("addr") + "*" + parent.children.get(2).attribute.get("addr"));
                                        tuple4Code.add("(*, " + parent.children.get(0).attribute.get("addr") + ", " + parent.children.get(2).attribute.get("addr") + ", " + parent.attribute.get("addr") + ")");
                                    } else {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: unmatched type " + parent.children.get(0).attribute.get("type") + " * " + parent.children.get(2).attribute.get("type"), parent.children.get(0).token.line));
                                    }
                                }
                                break;
                            } case 30: {
                                // E::=E1 + G {if G.type!=E1.type then error; else E.addr=newTemp(); E.type=E1.type; gen(E.addr=E1.addr+G.addr);}
                                if (parent.children.get(0).attribute.get("addr") == null || parent.children.get(2).attribute.get("addr") == null) {
                                    break;
                                } else {
                                    if (parent.children.get(0).attribute.get("type").equals(parent.children.get(2).attribute.get("type"))) {
                                        parent.attribute.put("addr", newTemp());
                                        parent.attribute.put("type", parent.children.get(0).attribute.get("type"));
                                        add3Code.add(parent.attribute.get("addr") + "=" + parent.children.get(0).attribute.get("addr") + "+" + parent.children.get(2).attribute.get("addr"));
                                        tuple4Code.add("(+, " + parent.children.get(0).attribute.get("addr") + ", " + parent.children.get(2).attribute.get("addr") + ", " + parent.attribute.get("addr") + ")");
                                    } else {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: unmatched type " + parent.children.get(0).attribute.get("type") + " + " + parent.children.get(2).attribute.get("type"), parent.children.get(0).token.line));
                                    }
                                }
                                break;
                            } case 29: {
                                // L::=IDN [ E ] {p=lookUp(IDN.lexeme);if p==null then error;
                                // else if E.addr!=null then if E.type!=int then error;
                                // else L.array=p; L.type=p.type; L.offset=newTemp(); gen(L.offset=E.addr*L.type.width);}
                                TreeNode symbol = lookUp(parent.children.get(0).token.str);
                                if (symbol != null) {
                                    if (parent.children.get(2).attribute.get("type") == null) {
                                        break;
                                    } else {
                                        if (parent.children.get(2).attribute.get("type").equals("int")) {
                                            parent.attribute.put("array", symbol.token.str);
                                            String type = symbol.attribute.get("type");
                                            String dim = type.split(",")[0].split("\\(")[1];
                                            type = type.substring(type.indexOf(type.split(",")[1]), type.length()-1).trim();
                                            parent.attribute.put("type", type);
                                            parent.attribute.put("offset", newTemp());
                                            parent.attribute.put("dim", dim);
                                            add3Code.add(parent.attribute.get("offset") + "=" + parent.children.get(2).attribute.get("addr") + "*4");
                                            tuple4Code.add("(*, " + parent.children.get(2).attribute.get("addr") + ", 4, " + parent.attribute.get("offset") +")");
                                        } else {
                                            semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(2).token.line + "]: unmatched type for index of array " + parent.children.get(2).attribute.get("type"), parent.children.get(2).token.line));
                                        }
                                    }
                                } else {
                                    semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: undefined symbol " + parent.children.get(0).token.str, parent.children.get(0).token.line));
                                }
                                break;
                            } case 28: {
                                // L::=L1 [ E ] {if E.addr!=null then if E.type!=int then error;
                                // else L.array=L1.array; L.type=L1.type; tmp=newTemp(); gen(tmp=E.addr*L.type.width); L.offset=newTemp(); gen(L.offset=L1.offset+tmp);}
                                if (parent.children.get(2).attribute.get("addr") != null) {
                                    if (parent.children.get(2).attribute.get("type").equals("int")) {
                                        parent.attribute.put("array", parent.children.get(0).attribute.get("array"));
                                        String type = parent.children.get(0).attribute.get("type");
                                        type = type.substring(type.indexOf(type.split(",")[1]), type.length()-1).trim();
                                        parent.attribute.put("type", type);
                                        String tmp = newTemp();
                                        add3Code.add(tmp + "=" + parent.children.get(0).attribute.get("offset") + "*" + parent.children.get(0).attribute.get("dim"));
                                        tuple4Code.add("(*, " + parent.children.get(0).attribute.get("offset")  + ", " + parent.children.get(0).attribute.get("dim") + ", " + tmp + ")");
                                        parent.attribute.put("offset", newTemp());
                                        String width = String.valueOf(Integer.parseInt(parent.children.get(2).attribute.get("addr")) * 4);
                                        add3Code.add(parent.attribute.get("offset") + "=" + tmp + "+" + width);
                                        tuple4Code.add("(+, " + tmp + ", " + width + ", " + parent.attribute.get("offset") + ")");
                                    } else {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(2).token.line + "]: unmatched type for index of array " + parent.children.get(2).attribute.get("type"), parent.children.get(2).token.line));
                                    }
                                } else {
                                    break;
                                }
                                break;
                            } case 18 :{
                                parent.attribute.put("nextList", "");
                                // S::=L = E ; {if L.array!=null or E.addr!=null then
                                // if L.type!=E.type then error;
                                // else gen(L.array[L.offset]=E.addr);}
                                if (parent.children.get(0).attribute.get("array") == null || parent.children.get(2).attribute.get("addr") == null) {
                                    break;
                                } else {
                                    if (!parent.children.get(0).attribute.get("type").equals(parent.children.get(2).attribute.get("type"))) {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: unmatched type " + parent.children.get(0).attribute.get("type") + " = " + parent.children.get(2).attribute.get("type"), parent.children.get(0).token.line));
                                    } else {
                                        add3Code.add(parent.children.get(0).attribute.get("array") + "[" + parent.children.get(0).attribute.get("offset") + "]=" + parent.children.get(2).attribute.get("addr"));
                                        tuple4Code.add("(=, " + parent.children.get(2).attribute.get("addr") + ", _, " + parent.children.get(0).attribute.get("array") + "[" + parent.children.get(0).attribute.get("offset") + "])");
                                    }
                                }
                                break;
                            } case 19: {
                                parent.attribute.put("nextList", "");
                                // S::=IDN = E ; {p=lookUp(IDN.lexeme); if p==null then error;
                                // else if p.type!=E.type then error;
                                // else gen(p=E.addr);}
                                TreeNode symbol = lookUp(parent.children.get(0).token.str);
                                if (symbol != null) {
                                    if (parent.children.get(2).attribute.get("addr") == null) {
                                        break;
                                    }
                                    if (!symbol.attribute.get("type").equals(parent.children.get(2).attribute.get("type"))) {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: unmatched type " + parent.children.get(0).attribute.get("type") + " = " + parent.children.get(2).attribute.get("type"), parent.children.get(0).token.line));
                                    } else {
                                        add3Code.add(symbol.token.str + "=" + parent.children.get(2).attribute.get("addr"));
                                        tuple4Code.add("(=, " + parent.children.get(2).attribute.get("addr") + ",  _, " + symbol.token.str + ")");
                                    }
                                } else {
                                    semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(0).token.line + "]: undefined symbol " + parent.children.get(0).token.str, parent.children.get(0).token.line));
                                }
                                break;
                            } case 25: {
                                parent.attribute.put("nextList", "");
                                // S::=RETURN E ; {gen(return E.addr);}
                                add3Code.add("return " + parent.children.get(1).attribute.get("addr"));
                                tuple4Code.add("(return, _, _, " + parent.children.get(1).attribute.get("addr") + ")");
                                break;
                            }
                            // call
                            case 57: {
                                // EList::=E {EList.size=1; EList.types=E.type; EList.params=E.addr}
                                parent.attribute.put("size", "1");
                                parent.attribute.put("types", parent.children.get(0).attribute.get("type"));
                                parent.attribute.put("params", parent.children.get(0).attribute.get("addr"));
                                break;
                            } case 56: {
                                // EList::=EList1 , E {EList.size=EList1.size+1; EList.types=EList1.types+E.type; EList.params=EList1.params+E.addr;}
                                parent.attribute.put("size", String.valueOf(1 + Integer.parseInt(parent.children.get(0).attribute.get("size"))));
                                parent.attribute.put("types", parent.children.get(0).attribute.get("types") + ";" + parent.children.get(2).attribute.get("type"));
                                parent.attribute.put("params", parent.children.get(0).attribute.get("params") + ";" + parent.children.get(2).attribute.get("addr"));
                                break;
                            } case 24: {
                                parent.attribute.put("nextList", "");
                                // S::=CALL IDN ( EList ) ; {p=lookUp(IDN.lexeme); if p==null || p.type!=proc then error;
                                // else if IDN.types!=EList.types then error;
                                // else n=0; for E in EList {gen(param E.addr); n=n+1;} gen(call IDN.lexeme , n)}
                                TreeNode symbol = lookUp(parent.children.get(1).token.str);
                                if (symbol != null && symbol.attribute.get("type").equals("proc")) {
                                    if (!symbol.attribute.get("types").equals(parent.children.get(3).attribute.get("types"))) {
                                        semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(3).token.line + "]: unmatched types " + parent.children.get(3).attribute.get("types"), parent.children.get(3).token.line));
                                    } else {
                                        int n = 0;
                                        for (String param : parent.children.get(3).attribute.get("params").split(";")) {
                                            add3Code.add("param: " + param);
                                            tuple4Code.add("(j, _, _, " + param + ")");
                                            n++;
                                        }
                                        add3Code.add("call " + symbol.token.str + ", " + n);
                                        tuple4Code.add("(call, " + n + ", _, " +symbol.token.str + ")");
                                    }
                                } else {
                                    semanticErrors.add(new ErrorInfo("Semantic error at Line [" + parent.children.get(1).token.line + "]: undefined proc " + parent.children.get(1).token.str, parent.children.get(1).token.line));
                                }
                                break;
                            }
                            // switch and loop
                            case 50: {
                                // R::=LT {R.val=<;}
                                parent.attribute.put("val", "<");
                                break;
                            } case 51: {
                                // R::=GT {R.val=>;}
                                parent.attribute.put("val", ">");
                                break;
                            } case 52: {
                                // R::=LE {R.val=<=;}
                                parent.attribute.put("val", "<=");
                                break;
                            } case 53: {
                                // R::=GE {R.val=<=;}
                                parent.attribute.put("val", ">=");
                                break;
                            } case 54: {
                                // R::=NE {R.val=!=;}
                                parent.attribute.put("val", "!=");
                                break;
                            } case 55: {
                                // R::=EQ {R.val===;}
                                parent.attribute.put("val", "==");
                                break;
                            } case 47: {
                                // I::=E1 R E2 {I.trueList=makeList(nextQuad()); I.falseList=makeList(nextQuad()+1);
                                // gen(if E1.addr R.val E2.addr goto); gen(goto);}
                                parent.attribute.put("trueList", makeList(nextQuad()));
                                parent.attribute.put("falseList", makeList(nextQuad()+1));
                                add3Code.add("if " + parent.children.get(0).attribute.get("addr") + parent.children.get(1).attribute.get("val") + parent.children.get(2).attribute.get("addr") + " goto");
                                tuple4Code.add("(j" + parent.children.get(1).attribute.get("val") + " , " + parent.children.get(0).attribute.get("addr") + ", " + parent.children.get(2).attribute.get("addr") + ", )");
                                add3Code.add("goto");
                                tuple4Code.add("(j, _, _, )");
                                break;
                            } case 48: {
                                // I::=TRUE {T.trueList=makeList(nextQuad()); gen(goto);}
                                parent.attribute.put("trueList", makeList(nextQuad()));
                                add3Code.add("goto");
                                tuple4Code.add("(j, _, _, )");
                                break;
                            } case 49: {
                                // I::=FALSE {T.falseList=makeList(nextQuad()); gen(goto);}
                                parent.attribute.put("falseList", makeList(nextQuad()));
                                add3Code.add("goto");
                                tuple4Code.add("(j, _, _, )");
                                break;
                            } case 46: {
                                // I::=( B )
                                parent.attribute.put("trueList", parent.children.get(1).attribute.get("trueList"));
                                parent.attribute.put("falseList", parent.children.get(1).attribute.get("falseList"));
                                break;
                            } case 45: {
                                // I::=! I1
                                parent.attribute.put("trueList", parent.children.get(1).attribute.get("falseList"));
                                parent.attribute.put("falseList", parent.children.get(1).attribute.get("trueList"));
                                break;
                            } case 44: case 42: {
                                // H::=I
                                // B::=H
                                parent.attribute.put("trueList", parent.children.get(0).attribute.get("trueList"));
                                parent.attribute.put("falseList", parent.children.get(0).attribute.get("falseList"));
                                break;
                            } case 43: {
                                // H::=H1 && BM I {backPatch(H1.trueList, BM.quad); H.trueList=I.trueList; H.falseList=merge(H1.falseList, I.falseList);}
                                backPatch(turnList(parent.children.get(0).attribute.get("tureList")), Integer.parseInt(parent.children.get(2).attribute.get("quad")));
                                parent.attribute.put("trueList", parent.children.get(3).attribute.get("trueList"));
                                parent.attribute.put("falseList", merge(turnList(parent.children.get(0).attribute.get("falseList")), turnList(parent.children.get(3).attribute.get("falseList"))));
                                break;
                            } case 41: {
                                // B::=B1 || BM H  {backPatch(B1.falseList, BM.quad); B.trueList=merge(B1.trueList, H.trueList); B.falseList=H.falseList;}
                                backPatch(turnList(parent.children.get(0).attribute.get("falseList")), Integer.parseInt(parent.children.get(2).attribute.get("quad")));
                                parent.attribute.put("trueList", merge(turnList(parent.children.get(0).attribute.get("trueList")), turnList(parent.children.get(3).attribute.get("trueList"))));
                                parent.attribute.put("falseList", parent.children.get(3).attribute.get("falseList"));
                                break;
                            } case 26: {
                                // BM::= {BM.quad=nextQuad();}
                                parent.attribute.put("quad", String.valueOf(nextQuad()));
                                break;
                            } case 27: {
                                // N::= {N.nextList=makeList(nextQuad()); gen(goto);}
                                parent.attribute.put("nextList", makeList(nextQuad()));
                                add3Code.add("goto");
                                tuple4Code.add("(j, _, _, )");
                                break;
                            } case 22: {
                                // S::=IF ( B ) BM1 THEN S1 N ELSE BM2 S2 {
                                // backPatch(B.trueList, BM1.quad);
                                // backPatch(B.falseList, BM2.quad);
                                // tmp=merge(S1.nextList, N.nextList);
                                // S.nextList=merge(tmp, S2.nextList);}
                                backPatch(turnList(parent.children.get(2).attribute.get("trueList")), Integer.parseInt(parent.children.get(4).attribute.get("quad")));
                                backPatch(turnList(parent.children.get(2).attribute.get("falseList")), Integer.parseInt(parent.children.get(9).attribute.get("quad")));
                                String tmp = merge(turnList(parent.children.get(6).attribute.get("nextList")), turnList(parent.children.get(7).attribute.get("nextList")));
                                parent.attribute.put("nextList", merge(turnList(tmp), turnList(parent.children.get(10).attribute.get("nextList"))));
                                break;
                            } case 23: {
                                // S::=WHILE BM1 ( B ) DO BM2 S1 {
                                // backPatch(S1.nextList, BM1.quad);
                                // backPatch(B.trueList, BM2.quad);
                                // S.nextList = B.falseList;
                                // gen(goto BM1.quad);}
                                backPatch(turnList(parent.children.get(7).attribute.get("nextList")), Integer.parseInt(parent.children.get(1).attribute.get("quad")));
                                backPatch(turnList(parent.children.get(3).attribute.get("trueList")), Integer.parseInt(parent.children.get(6).attribute.get("quad")));
                                parent.attribute.put("nextList", parent.children.get(3).attribute.get("falseList"));
                                add3Code.add("goto" + parent.children.get(1).attribute.get("quad"));
                                tuple4Code.add("(j, _, _, " + parent.children.get(1).attribute.get("quad") +")");
                                break;
                            }
                        }

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

    private boolean enter(String idLexeme, String type, String offset, int line) {
        if (lookUp(idLexeme) != null) {
            semanticErrors.add(new ErrorInfo("Semantic error at Line [" + line + "]: repeat declaration " + idLexeme, line));
            return false;
        } else {
            TreeNode symbol = new TreeNode(symbols.size()+1, new Token(idLexeme, new String[]{"IDN", idLexeme}, line), true);
            symbol.attribute.put("type", type);
            symbol.attribute.put("offset", offset);
            symbols.add(symbol);
            return true;
        }
    }

    private TreeNode lookUp(String lexeme) {
        for (TreeNode symbol : symbols) {
            if (symbol.token.str.equals(lexeme)) {
                return symbol;
            }
        }
        return null;
    }

    private String newTemp() {
        return "t" + (++tmpNum);
    }

    private int nextQuad() {
        return add3Code.size();
    }

    private String merge(List<Integer> list1, List<Integer> list2) {
        List<Integer> list = new ArrayList<>();
        for (int i : list1) {
            if (!list.contains(i)) {
                list.add(i);
            }
        }
        for (int i : list2) {
            if (!list.contains(i)) {
                list.add(i);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            int quad = list.get(i);
            builder.append(quad);
            if (i < list.size()-1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private String makeList(int quad) {
        return "" + quad;
    }

    private void backPatch(List<Integer> list, int quad) {
        for (int i : list) {
            String patched3 = add3Code.get(i).replace("goto", "goto" + quad);
            String patched4 = tuple4Code.get(i).replace(")", quad + ")");
            add3Code.set(i, patched3);
            tuple4Code.set(i, patched4);
        }
    }

    private List<Integer> turnList(String list) {
        List<Integer> integerList = new ArrayList<>();
        if (list.isEmpty()) {
            return integerList;
        }
        if (!list.contains(",")) {
            integerList.add(Integer.parseInt(list));
            return integerList;
        }
        for (String num : list.split(",")) {
            integerList.add(Integer.parseInt(num));
        }
        return integerList;
    }
}
