package lexical;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DFA {

    private int N;              // number of state
    private int M;              // number of symbol
    private char[] sigma;       // symbol set
    private int[][] delta;      // transfer function
    private int s;              // initial state
    private List<Integer> f;    // final state

    public DFA() {
        /* parse from json file */
        try {
            JSONParser parser = new JSONParser();
            JSONObject object = (JSONObject) parser.parse(new FileReader("src/main/java/lexical/dfa.json"));
            /* initialize state number and symbol number */
            this.N = Integer.parseInt((String) object.get("stateNumber"));
            this.M = Integer.parseInt((String) object.get("symbolNumber"));
            /* initialize sigma */
            this.sigma = new char[M];
            String symbol = (String) object.get("sigma");
            for (int i = 0; i < symbol.length(); i++) {
                this.sigma[i] = symbol.charAt(i);
            }
            /* initialize transfer function */
            this.delta = new int[N][M];
            // initialize using -1
            int[] minus = new int[M];
            Arrays.fill(minus, -1);
            Arrays.fill(delta, minus);
            // TODO parse from json

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
