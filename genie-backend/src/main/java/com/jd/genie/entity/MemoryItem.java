package com.jd.genie.entity;



import com.jd.genie.memory.MemoryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;


import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemoryItem {

    private String id;
    private String content;
    private MemoryType memoryType;
    private String userId;
    private DateTime timeStamp;
    private float importance;
    private Map<String, Object> metadata;
}
