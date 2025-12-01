package com.jd.genie.memory;

import com.jd.genie.entity.MemoryItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoredMemory {
    private MemoryItem memoryItem;
    private double score;
}
