package Node;

public enum EntranceEnum {
    E3;

    public String toString() {
        return this.name();
    }

    public static EntranceEnum toEntranceEnum(String entranceStr) {
        for (EntranceEnum entrance : EntranceEnum.values()) {
            if (entrance.toString().equals(entranceStr)) {
                return entrance;
            }
        }
        return null;
    }
}
