package net.scheffers.robot.xasm.isa;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstructionSet {
    
    public InstructionDef[] instructions;
    public Map<String, InstructionDef[]> firstTokenMap;
    public String[] forbiddenNames;
    public int wordBits;
    
    public InstructionSet() {
        
    }
    
    public InstructionSet(File file) throws Exception {
        JSONObject obj = new JSONObject(new String(Files.readAllBytes(file.toPath())));
        // Metadata.
        JSONObject meta = obj.getJSONObject("metadata");
        wordBits = meta.getInt("word_size");
        // Instructions.
        JSONArray instructions = obj.getJSONArray("instructions");
        this.instructions = new InstructionDef[instructions.length()];
        for (int i = 0; i < instructions.length(); i++) {
            // Get the JSONObject corresponding to the instruction.
            JSONObject insn = instructions.getJSONObject(i);
            // Let the constructor handle the rest.
            this.instructions[i] = new InstructionDef(insn, wordBits);
        }
        // Instruction of mappings and forbidden names.
        recalcMap();
    }
    
    public boolean isValidForExpression(String in) {
        for (String no : forbiddenNames) {
            if (no.equalsIgnoreCase(in)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean matchValidLabel(String s) {
        return s.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }
    
    public static boolean matchValidLabelUse(String s) {
        return s.matches("[<>]?.?[a-zA-Z_][a-zA-Z0-9_]*");
    }
    
    public void recalcMap() {
        firstTokenMap = new HashMap<>();
        Set<String> youShallNotPass = new HashSet<>(); // Set of illegal label names.
        for (InstructionDef def : instructions) {
            // Add the tokens to the illegal label names if they would otherwise be valid.
            for (String tkn : def.tokenPattern) {
                if (matchValidLabel(tkn)) {
                    youShallNotPass.add(tkn.toLowerCase());
                }
            }
            // Get the first token.
            String first = def.tokenPattern[0].toLowerCase();
            // Check if it is already in the map.
            if (firstTokenMap.containsKey(first)) {
                // If so, add it to the existing array there.
                InstructionDef[] old = firstTokenMap.get(first);
                InstructionDef[] dem = new InstructionDef[old.length + 1];
                System.arraycopy(old, 0, dem, 0, old.length);
                dem[old.length] = def;
                firstTokenMap.put(first, dem);
            }
            else
            {
                // If not, add a new entry.
                firstTokenMap.put(first, new InstructionDef[]{def});
            }
        }
        // Turn the set into an array.
        forbiddenNames = youShallNotPass.toArray(new String[0]);
    }
    
    public boolean isValidLabelName(String s) {
        // Check for valid label structure.
        if (!matchValidLabel(s)) {
            // Not matched, label name is invalid.
            return false;
        }
        // Exclude certain label names from being valid.
        for (String youShallNotPass : forbiddenNames) {
            if (s.equalsIgnoreCase(youShallNotPass)) {
                // Matched, label name is invalid.
                return false;
            }
        }
        // Label name is valid.
        return true;
    }
    
}
