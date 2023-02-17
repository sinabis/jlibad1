package ad1.content;

import java.util.HashMap;

public enum AD1Types {
    FILE(0),
    // NTFS_Index_AllocationIndex(1),
    DIRECTORY(5);

    private final int value;
    private static final HashMap<Integer, AD1Types> map = new HashMap<>();

    AD1Types(int value) {
        this.value = value;
    }

    static {
        for (AD1Types type : values()) {
            map.put(type.value, type);
        }
    }

    public static AD1Types valueOf(int pageType) {
        return map.get(pageType);
    }

    public long getValue() {
        return value;
    }
}
