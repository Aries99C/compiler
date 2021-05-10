package grammatical;

public class TableEntry {
    // action == 1 -> shift
    // action == 2 -> reduce
    // action == 3 -> goto
    // action == 4 -> acc
    public int value;
    public int symbolIndex;
    public int action;
}
