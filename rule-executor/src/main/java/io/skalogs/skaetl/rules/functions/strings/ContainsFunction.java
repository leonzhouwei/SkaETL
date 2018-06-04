package io.skalogs.skaetl.rules.functions.strings;

import io.skalogs.skaetl.rules.functions.VarArgFilterFunction;

import java.util.List;

public class ContainsFunction extends VarArgFilterFunction<String> {
    public ContainsFunction() {
        super("evaluates whether the string contains the specified strings", "myfield CONTAINS(\"a\",\"b\")");
    }

    @Override
    public Boolean evaluateVarArgs(String fieldValue, List<String> values) {
        if (fieldValue == null) {
            return false;
        }
        for (String value : values) {
            if (fieldValue.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
