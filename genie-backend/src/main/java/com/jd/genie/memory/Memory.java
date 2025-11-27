package com.jd.genie.memory;


import com.jd.genie.config.MemoryConfig;
import com.jd.genie.entity.MemoryItem;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public abstract class Memory {

    private MemoryConfig config;
//    @Value("${storage.backend}")
//    private String storageBackend;
    private String memoryType;

    public Memory(MemoryConfig config){
        this.config = config;
        this.memoryType = this.getClass().getSimpleName().toLowerCase().replace("memory", "");
    }

    //添加记忆
    public abstract String add(MemoryItem memoryItem);
    //检索记忆
    public abstract List<MemoryItem> retrieve(String Query, int limit, Map<String, Object> params);
    //更新记忆
    public abstract Boolean update(String memoryId, String content, float importance, Map<String, Object> metadata);
    //删除记忆
    public abstract Boolean delete(String memoryId);
    //检查记忆是否存在
    public abstract Boolean hasMemory(String memoryId);
    //清空记忆
    public abstract void clear();
    //获取记忆统计信息
    public abstract HashMap<String, Object> getStats();
    //生成记忆id
    public String generateMemoryId(){
        return UUID.randomUUID().toString();
    }
    //计算记忆重要性
    public double calculateImportance(String content, float base_importance){
        float importance = base_importance;
        if(content.length() > 100){
            importance += 0.1;
        }
        String[] important_keywords = {"重要", "关键", "必须", "注意", "警告", "错误"};
        for(String keyword : important_keywords){
            if(content.contains(keyword)){
                importance += 0.2;
            }
        }
        return Math.max(0.0, Math.min(1.0, importance));
    }
    @Data
    public class MemoryEntry<T>{
        private final double priority;
        private final long timeStamp;
        private final T memory;
        public MemoryEntry(double priority, long timeStamp, T memory) {
            this.priority = priority;
            this.timeStamp = timeStamp;
            this.memory = memory;
        }
    }
}
