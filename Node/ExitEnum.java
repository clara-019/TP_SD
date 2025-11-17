package Node;

public enum ExitEnum {
    S;

    public String toString() {
        return this.name();
    }

    public static ExitEnum toExitEnum(String exitStr) {
        for (ExitEnum exit : ExitEnum.values()) {
            if (exit.toString().equals(exitStr)) {
                return exit;
            }
        }
        return null;
    }
}