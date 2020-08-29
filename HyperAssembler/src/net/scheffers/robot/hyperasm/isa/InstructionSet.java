package net.scheffers.robot.hyperasm.isa;

import java.util.*;

public class InstructionSet {
    
    public InstructionDef[] instructions;
    public Map<String, InstructionDef[]> firstTokenMap;
    public String[] forbiddenNames;
    public int wordBits;
    
    public void recalcMap() {
        firstTokenMap = new HashMap<>();
        Set<String> youShallNotPass = new HashSet<>();
        for (InstructionDef def : instructions) {
            for (String tkn : def.tokenPattern) {
                youShallNotPass.add(tkn.toLowerCase());
            }
            String first = def.tokenPattern[0].toLowerCase();
            if (firstTokenMap.containsKey(first)) {
                InstructionDef[] old = firstTokenMap.get(first);
                InstructionDef[] dem = new InstructionDef[old.length + 1];
                System.arraycopy(old, 0, dem, 0, old.length);
                dem[old.length] = def;
                firstTokenMap.put(first, dem);
            }
            else
            {
                firstTokenMap.put(first, new InstructionDef[]{def});
            }
        }
        forbiddenNames = youShallNotPass.toArray(new String[0]);
    }
    
    public boolean isValidLabelName(String s) {
        if (!s.matches("[a-zA-Z0-9_]+")) {
            return false;
        }
        for (String youShallNotPass : forbiddenNames) {
            if (s.equalsIgnoreCase(youShallNotPass)) {
                return false;
            }
        }
        return true;
    }
    
}
