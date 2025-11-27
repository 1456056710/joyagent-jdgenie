package com.jd.genie.memory;

import com.jd.genie.config.MemoryConfig;
import com.jd.genie.entity.MemoryItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MemoryManager {

    private MemoryConfig memoryConfig;
    private boolean isWorkingMemory;
    private String userId;
    private Map<MemoryType, Memory> memoryTypes = new EnumMap<>(MemoryType.class);

    public MemoryManager(MemoryConfig memoryConfig, Optional<WorkingMemory> workingMemory) {
        this.memoryConfig = memoryConfig;
        workingMemory.ifPresent(memory -> memoryTypes.put(MemoryType.WORKING, memory));

        log.info("MemoryManager init success, loaded: {}", memoryTypes.keySet());
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String addMemory(String content, float importance, Map<String, Object> metadata){
        MemoryType type = classifyMemory(content, metadata);
        MemoryItem item = new MemoryItem("", content, type, this.userId, null, -1, metadata);
        Memory memory = memoryTypes.get(type);
        String memoryId = memory.add(item);
        log.info("add memory: {}", item);
        return memoryId;
    }

    public List<MemoryItem> retrieveMemory(String query, int limit, Map<String, Object> params){
        if(params == null || params.isEmpty()) return List.of();
        if(params.containsKey("type")){
            Object o = params.get("type");
            if(o instanceof MemoryType){
                Memory memory = memoryTypes.get((MemoryType) o);
                return memory.retrieve(query, limit, params);
            }else if(o instanceof List){
                List<MemoryItem> result = new ArrayList<>();
                for(MemoryType type : (List<MemoryType>) o){
                    Memory memory = memoryTypes.get(type);
                    result.addAll(memory.retrieve(query, limit, params));
                }
                return result;
            }
        }
        return List.of();
    }

    public Boolean updateMemory(String memoryId, String content, float importance, Map<String, Object> metadata){
        for(Memory memory : memoryTypes.values()){
            if(memory.hasMemory(memoryId)){
                return memory.update(memoryId, content, importance, metadata);
            }
        }
        return false;
    }

    public Boolean deleteMemory(String memoryId){
        for(Memory memory : memoryTypes.values()){
            if(memory.hasMemory(memoryId)){
                return memory.delete(memoryId);
            }
        }
        return false;
    }

    private MemoryType classifyMemory(String content, Map<String, Object> metadata) {
        if(metadata != null && metadata.containsKey("type")){
            return MemoryType.valueOf(metadata.get("type").toString());
        }
        if (episodicContent(content)){
            return MemoryType.EPISODIC;
        }else if (semanticContent(content)){
            return MemoryType.SEMANTIC;
        }else if (perceptualContent(content)){
            return MemoryType.PERCEPTUAL;
        }else {
            return MemoryType.WORKING;
        }
    }

    private boolean perceptualContent(String content) {
        List<String> modalities = Arrays.asList("text", "image", "audio", "video");
        return modalities.stream().anyMatch(modality -> content.contains(modality));
    }

    private boolean semanticContent(String content) {
        List<String> keywords = Arrays.asList("如何", "为什么", "怎么", "如何做", "如何处理", "如何解决", "如何操作", "如何使用", "如何进行", "如何进行操作", "如何进行使用", "如何进行解决", "如何进行处理", "如何进行操作处理", "如何进行操作使用", "如何进行操作解决", "如何进行操作处理解决");
        return keywords.stream().anyMatch(keyword -> content.contains(keyword));
    }

    private boolean episodicContent(String content) {
        List<String> keywords = Arrays.asList("今天", "昨天", "明天", "本周", "本月", "今年", "最近", "近期");
        return keywords.stream().anyMatch(keyword -> content.contains(keyword));
    }
}
