package lexical;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DFA {

    private int M;                          // number of symbol
    private char[] sigma;                   // symbol set
    private int[][] delta;                  // transfer function
    private int s;                          // initial state
    private List<Integer> f;                // final state
    private List<Integer> e;                // error state
    private Set<String> keywords;           // key words
    private Map<Integer, String> finalInfo; // info for final state
    private Map<Integer, String> errorInfo; // info for error state
    private List<Token> tokens;             // save tokens
    private List<ErrorInfo> errors;         // save errors

    @SuppressWarnings("unchecked")
    public DFA() {
        /* parse from json file */
        try {
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(new FileReader("src/main/java/lexical/dfa.json"));
            /* initialize state number and symbol number */
            // number of state
            int n = Integer.parseInt((String) object.get("stateNumber"));
            this.M = Integer.parseInt((String) object.get("symbolNumber"));
            /* initialize initial state and final states */
            this.s = Integer.parseInt((String) object.get("s"));
            this.f = new ArrayList<>();
            JSONArray finalStates = (JSONArray) object.get("f");
            for (String finalState : (Iterable<String>) finalStates) {
                f.add(Integer.parseInt(finalState));
            }
            /* initialize sigma */
            this.sigma = new char[M];
            String symbol = (String) object.get("sigma");
            for (int i = 0; i < symbol.length(); i++) {
                this.sigma[i] = symbol.charAt(i);
            }
            /* initialize transfer function */
            this.delta = new int[n][M];
            // initialize using -1
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < M; j++) {
                    this.delta[i][j] = Integer.MIN_VALUE;
                }
            }
            // parse from json
            JSONObject transfer = (JSONObject) object.get("state");
            for (int i = 0; i < n; i++) {
                JSONObject state = (JSONObject) transfer.get(String.valueOf(i));
                if (state == null) {
                    continue;
                }
                Set<String> keys = state.keySet();
                for (String key : keys) {
                    for (int j = 0; j < key.length(); j++) {
                        this.delta[i][getIndexOfChar(key.charAt(j))] = Integer.parseInt((String) state.get(key));
                    }
                }
            }
            /* initialize key words */
            this.keywords = new HashSet<>();
            JSONArray keywords = (JSONArray) object.get("keyword");
            for (String keyword : (Iterable<String>) keywords) {
                this.keywords.add(keyword);
            }
            /* initialize final info */
            this.finalInfo = new HashMap<>();
            JSONObject finalMap = (JSONObject) object.get("final_info");
            for (int finalState : f) {
                this.finalInfo.put(finalState, (String) finalMap.get(String.valueOf(finalState)));
            }
            /* initialize error info and error states */
            this.e = new ArrayList<>();
            this.errorInfo = new HashMap<>();
            JSONObject errorMap = (JSONObject) object.get("error_info");
            for (String errorState : (Iterable<String>) errorMap.keySet()) {
                e.add(Integer.parseInt(errorState));
                this.errorInfo.put(Integer.parseInt(errorState), (String) errorMap.get(errorState));
            }
            /* initialize token table and error table */
            this.tokens = new ArrayList<>();
            this.errors = new ArrayList<>();
        } catch (FileNotFoundException e) {
            System.out.println("json file cannot be found.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("parse failed.");
            e.printStackTrace();
        }
    }

    public void run(char[] input) {
        int forwardIndex = -1;                  // save index of latest final state
        int forwardState = Integer.MIN_VALUE;   // save latest final state
        int forwardLine = 1;                    // save latest line
        String forwardStr = "";                 // save latest string
        String currentStr = "";                 // save current string
        int currentLine = 1;                    // save current line
        int currentState = this.s;
        for (int i = 0; i < input.length; i++) {
            char nextChar = input[i];
            // EOF
            if (nextChar == '$' && currentStr.isEmpty()) {
                break;
            }
            int nextState = nextState(currentState, nextChar);
            /* successor state exists */
            if (nextState != Integer.MIN_VALUE) {
                currentState = nextState;
                currentStr = currentStr + nextChar;
                if (currentStr.length() == 1) {
                    currentStr = currentStr.trim();
                }
                // next line
                if (nextChar == '\n') currentLine++;
                /* successor state is final */
                if (f.contains(currentState)) {
                    forwardIndex = i;
                    forwardState = currentState;
                    forwardStr = currentStr;
                    forwardLine = currentLine;
                }
                /* successor state is error */
                if (e.contains(currentState)) {
                    errors.add(new ErrorInfo(errorInfo.get(currentState), getLine(input, i)));
                }
            }
            /* no successor state */
            else {
                // back
                if (forwardState > 0) {
                    // keyword
                    if (keywords.contains(forwardStr)) {
                        tokens.add(new Token(forwardStr, new String[]{forwardStr.toUpperCase(Locale.ROOT), "_"}));
                    }
                    // identifier
                    else if (finalInfo.get(forwardState).equals("ID")) {
                        tokens.add(new Token(forwardStr, new String[]{"ID", forwardStr}));
                    }
                    // number
                    else if (finalInfo.get(forwardState).equals("DINT")) {
                        tokens.add(new Token(forwardStr, new String[]{"DINT", forwardStr}));
                    }
                    else if (finalInfo.get(forwardState).equals("OCT")) {
                        tokens.add(new Token(forwardStr, new String[]{"OCT", forwardStr}));
                    }
                    else if (finalInfo.get(forwardState).equals("HEX")) {
                        tokens.add(new Token(forwardStr, new String[]{"HEX", forwardStr}));
                    }
                    else if (finalInfo.get(forwardState).equals("FP")) {
                        tokens.add(new Token(forwardStr, new String[]{"FP", forwardStr}));
                    }
                    // string and char
                    else if (finalInfo.get(forwardState).equals("STR")) {
                        tokens.add(new Token(forwardStr, new String[]{"STR", forwardStr}));
                    }
                    else if (finalInfo.get(forwardState).equals("CH")) {
                        tokens.add(new Token(forwardStr, new String[]{"CH", forwardStr}));
                    }
                    // single comment
                    else if (finalInfo.get(forwardState).equals("SCM")) {
                        // do nothing
                    }
                    // multiline comment
                    else if (finalInfo.get(forwardState).equals("MCM")) {
                        // do nothing
                    }
                    // else symbol
                    else {
                        tokens.add(new Token(forwardStr, new String[]{finalInfo.get(forwardState), "_"}));
                    }
                    // back to initial state
                    currentState = this.s;
                    currentStr = "";
                    i = forwardIndex;
                    currentLine = forwardLine;
                    forwardIndex = -1;
                    forwardState = Integer.MIN_VALUE;
                    forwardStr = "";
                }
                // cannot back
                else {
                    /* error handle */
                    // illegal symbol
                    if (getIndexOfChar(nextChar) == -1) {
                        errors.add(new ErrorInfo(errorInfo.get(-1) + ": " + nextChar, getLine(input, i)));
                        // panic-mode recovery
                        while (nextState(currentState, input[i]) == Integer.MIN_VALUE) {
                            i++;
                        }
                        i--;
                    }
                    // string not closed
                    else if (currentState == 10) {
                        errors.add(new ErrorInfo(errorInfo.get(-2) + ": " + currentStr, getLine(input, i)));
                        // panic-mode recovery
                        while (nextState(currentState, input[i]) == Integer.MIN_VALUE && input[i] != '\n' && i < input.length) {
                            i++;
                        }
                        i--;
                        // back to initial state
                        currentState = this.s;
                        currentStr = "";
                        currentLine = forwardLine;
                        forwardIndex = -1;
                        forwardState = Integer.MIN_VALUE;
                        forwardStr = "";
                    }
                    // char not closed
                    else if (currentState == 14) {
                        errors.add(new ErrorInfo(errorInfo.get(-3) + ": " + currentStr, getLine(input, i)));
                        // panic-mode recovery
                        while (nextState(currentState, input[i]) == Integer.MIN_VALUE && input[i] != '\n' && i < input.length) {
                            i++;
                        }
                        // back to initial state
                        currentState = this.s;
                        currentStr = "";
                        currentLine = forwardLine;
                        forwardIndex = -1;
                        forwardState = Integer.MIN_VALUE;
                        forwardStr = "";
                    }
                    // '\' except legal symbol
                    else if (currentState == 12 || currentState == 16) {
                        errors.add(new ErrorInfo(errorInfo.get(-4) + ": " + currentStr + nextChar, getLine(input, i)));
                        // panic-mode recovery
                        while (nextState(currentState, input[i]) == Integer.MIN_VALUE && input[i] != '\n' && i < input.length) {
                            i++;
                        }
                        i--;
                        // back to initial state
                        currentState = this.s;
                        currentStr = "";
                        currentLine = forwardLine;
                        forwardIndex = -1;
                        forwardState = Integer.MIN_VALUE;
                        forwardStr = "";
                    }
                    // comment not closed
                    else if (currentState == 62 || currentState == 63) {
                        errors.add(new ErrorInfo(errorInfo.get(-5) + " " + currentStr, getLine(input, i)));
                    }
                }
            }
        }
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<ErrorInfo> getErrors() {
        return errors;
    }

    private int getIndexOfChar(char c) {
        for (int i = 0; i < this.M; i++) {
            if (sigma[i] == c) {
                return i;
            }
        }
        return -1;
    }

    private int nextState(int currentState, char c) {
        int index = getIndexOfChar(c);
        if (index == -1) {
            return Integer.MIN_VALUE;
        } else {
            return delta[currentState][index];
        }
    }

    private int getLine(char[] input, int index) {
        int line = 1;
        int count = 0;
        for (char c : input) {
            count++;
            if (count == index) {
                return line;
            }
            if (c == '\n') {
                line++;
            }
        }
        return -1;
    }
}
