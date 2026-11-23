package biobuzz;

public enum Alliance {
    RED, BLUE;

    public Alliance opponent() {
        return this == RED ? BLUE : RED;
    }
}
