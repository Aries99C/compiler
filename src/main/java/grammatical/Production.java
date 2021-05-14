package grammatical;

import java.util.Arrays;
import java.util.Objects;

public class Production {
    public String left;
    public String[] rights;

    public Production(String left, String[] rights) {
        this.left = left;
        this.rights = rights;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(left).append(" ::= ");
        for (String right : rights) {
            if (!right.equals("epsilon")) {
                builder.append(right).append(" ");
            }
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return Objects.equals(left, that.left) && Arrays.equals(rights, that.rights);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(left);
        result = 31 * result + Arrays.hashCode(rights);
        return result;
    }
}
