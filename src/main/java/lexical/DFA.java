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

    private int N;              // number of state
    private int M;              // number of symbol
    private char[] sigma;       // symbol set
    private int[][] delta;      // transfer function
    private int s;              // initial state
    private List<Integer> f;    // final state

    @SuppressWarnings("unchecked")
    public DFA() {
        /* parse from json file */
        try {
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(new FileReader("src/main/java/lexical/dfa.json"));
            /* initialize state number and symbol number */
            this.N = Integer.parseInt((String) object.get("stateNumber"));
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
            this.delta = new int[N][M];
            // initialize using -1
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    this.delta[i][j] = -1;
                }
            }
            // parse from json
            JSONObject transfer = (JSONObject) object.get("state");
            for (int i = 0; i < N; i++) {
                System.out.println("count: " + i);
                JSONObject state = (JSONObject) transfer.get(String.valueOf(i));
                Set<String> keys = state.keySet();
                for (String key : keys) {
                    System.out.println("key of state " + i + " is " + key);
                    for (int j = 0; j < key.length(); j++) {
                        this.delta[i][getIndexOfChar(key.charAt(j))] = Integer.parseInt((String) state.get(key));
                    }
                }
            }
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
        int currentState = this.s;
        for (int i = 0; i < input.length; i++) {
            int nextState = nextState(currentState, input[i]);
            if (nextState == -1) {
                // TODO record Error

            } else {
                currentState = nextState;
            }
        }
    }

    private int nextState(int currentState, char c) {
        int index = getIndexOfChar(c);
        if (index == -1) {
            return -1;
        } else {
            return delta[currentState][index];
        }
    }

    private int getIndexOfChar(char c) {
        for (int i = 0; i < this.M; i++) {
            if (sigma[i] == c) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        DFA dfa = new DFA();
    }
}
