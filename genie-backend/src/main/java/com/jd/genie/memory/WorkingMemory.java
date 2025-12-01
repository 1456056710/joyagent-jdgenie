package com.jd.genie.memory;

import com.jd.genie.config.MemoryConfig;
import com.jd.genie.data.dto.VectorRecallReq;
import com.jd.genie.data.dto.VectorSaveReq;
import com.jd.genie.entity.MemoryItem;
import com.jd.genie.service.QdrantService;
import com.jd.genie.service.VectorService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
//import java.time.LocalDateTime;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Data
@Component
public class WorkingMemory extends Memory{

    private int maxCapacity;
    private int maxTokens;
    private int maxAgeMinutes;
    private int currentTokens;
    private DateTime sessionStartTime;
    private List<MemoryItem> memories;
    private PriorityQueue<MemoryEntry<MemoryItem>> memoryQueue; //(priority, timestamp, memory_item)

    private static final String COLLECTION = "working_memory";

    @Autowired
    private VectorService vectorService;
    @Autowired
    private QdrantService qdrantService;
//    @Autowired
//    private Memory memory;

    public WorkingMemory(MemoryConfig config) {
        super(config);
        this.maxCapacity = config.getMaxCapacity();
        this.maxTokens = config.getWorkingMemoryTokens();
        this.maxAgeMinutes = config.getWorkingMemoryTtlMinutes() != 0 ? config.getWorkingMemoryTtlMinutes() : 120;
        this.currentTokens = 0;
        this.sessionStartTime = DateTime.now();
        this.memories = new ArrayList<>();
        Comparator<MemoryEntry<MemoryItem>> cmp = Comparator
                .comparingDouble((MemoryEntry<MemoryItem> entry) -> entry.getPriority())
                .thenComparingLong(MemoryEntry::getTimeStamp);     // 时间早的先出队
        this.memoryQueue = new PriorityQueue<>(cmp);
    }

//    @PostConstruct
//    public void initCollection(){
//        try{
//            qdrantService.createCosineCollection(COLLECTION, 1024);
//        }catch (Exception e){
//            log.error("qdrant collection init failed");
//            throw new IllegalStateException("init working_memory collection failed", e);
//        }
//    }

    @Override
    public String add(MemoryItem memoryItem) {
        clearExpireMemories();
        if(StringUtils.isBlank(memoryItem.getId())){
            memoryItem.setId(generateMemoryId());
        }
        if(memoryItem.getImportance() <= 0){
            memoryItem.setImportance((float) calculateImportance(memoryItem.getContent(), 0.3f));
        }
        if(memoryItem.getTimeStamp() == null){
            memoryItem.setTimeStamp(DateTime.now());
        }
//        VectorSaveReq req = new VectorSaveReq();
//        req.setCollectionName(COLLECTION);
//        VectorSaveReq.VectorData vectorData = new VectorSaveReq.VectorData();
//        vectorData.setUuid(memoryItem.getId());
//        vectorData.setEmbeddingText(memoryItem.getContent());
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("id", memoryItem.getId());
//        payload.put("content", memoryItem.getContent());
//        payload.put("importance", memoryItem.getImportance());
//        payload.put("metadata", memoryItem.getMetadata());
//        payload.put("timestamp", memoryItem.getTimeStamp());
//        payload.put("memoryType", memoryItem.getMemoryType());
//        payload.put("userId", memoryItem.getUserId());
//        vectorData.setPayloads(payload);
//        req.setDataList(List.of(vectorData));
//        vectorService.saveVector(req);
        Double priority = calculatePriority(memoryItem);
        memoryQueue.offer(new MemoryEntry<>(priority, memoryItem.getTimeStamp().getMillis(), memoryItem));
//        double priority = calculatePriority(memoryItem);
//        this.memoryQueue.add(memoryItem);
        this.memories.add(memoryItem);
        this.currentTokens += memoryItem.getContent().length();
        capacityLimits();
        return memoryItem.getId();
    }

    @Override
    public List<MemoryItem> retrieve(String query, int limit, Map<String, Object> params) {
        clearExpireMemories();
        if(memories.isEmpty()) return Collections.emptyList();

        String userId = params != null && params.containsKey("userId") ? (String) params.get("userId") : null;
        List<MemoryItem> candidates = memories;
        if(StringUtils.isBlank(userId)){
            return Collections.emptyList();
        }
        candidates = memories.stream()
                .filter(m -> userId.equalsIgnoreCase(m.getUserId()))
                .toList();
        if(candidates.isEmpty()){
            return Collections.emptyList();
        }
        try{
            return luceneRetrieve(query, candidates, limit);
        } catch (Exception e){
            log.error("WorkingMemory lucene retrieve failed", e);
            return Collections.emptyList();
        }


//        VectorRecallReq req = new VectorRecallReq();
//        req.setCollectionName(COLLECTION);
//        req.setQuery(query);
//        req.setLimit(limit);
//        req.setTimeout(1500L);
//        req.setScoreThreshold(0.2f);
//        req.setPayloads(Arrays.asList("id","content","userId","memoryType","importance","metadata","timestamp"));
//        if (params != null && !params.isEmpty()) {
//            Map<String, Object> filter = new HashMap<>();
//            if (params.get("userId") != null) {
//                filter.put("userId", params.get("userId"));
//            }
//            if (params.get("memoryType") != null) {
//                filter.put("memoryType", params.get("memoryType"));
//            }
//            if (!filter.isEmpty()) {
//                req.setKeywordFilterMap(filter);
//            }
//        }
//        List<Map<String, Object>> hits = vectorService.vectorRecall(req);
//        if(hits == null || hits.isEmpty()) return Collections.emptyList();
//        return hits.stream()
//                .map(hit -> {
//                    MemoryItem memoryItem = new MemoryItem();
//                    memoryItem.setId((String) hit.getOrDefault("_id", hit.get("id")));
//                    memoryItem.setContent((String) hit.get("content"));
//                    memoryItem.setUserId((String) hit.get("userId"));
//                    memoryItem.setMemoryType(MemoryType.valueOf((String) hit.get("memoryType")));
//                    Object imp = hit.get("importance");
//                    if (imp != null) {
//                        memoryItem.setImportance(Float.parseFloat(String.valueOf(imp)));
//                    }
//                    Object ts = hit.get("timestamp");
//                    if (ts != null) {
//                        memoryItem.setTimeStamp(DateTime.parse(String.valueOf(ts)));
//                    }
//                    Object md = hit.get("metadata");
//                    if (md instanceof Map) {
//                        memoryItem.setMetadata(new HashMap<>((Map<String, Object>) md));
//                    }
//                    return memoryItem;
//                })
////                .sorted(Comparator.comparingDouble(m -> -((Number) hits.get("score")).doubleValue()))
//                .limit(limit)
//                .collect(Collectors.toList());

    }

    private List<MemoryItem> luceneRetrieve(String query, List<MemoryItem> candidates, int limit) throws Exception{
        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try(IndexWriter writer = new IndexWriter(directory, config)){
            for(int i = 0; i < candidates.size(); i++){
                MemoryItem item = candidates.get(i);
                Document doc = new Document();
                doc.add(new StringField("index", String.valueOf(i), Field.Store.YES));
                doc.add(new TextField("content", item.getContent(), Field.Store.NO));
                writer.addDocument(doc);
            }
        }
        List<ScoredMemory> scoredResults = new ArrayList<>();
        try(IndexReader reader = DirectoryReader.open(directory)){
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);

            Query luceneQuery = parser.parse(QueryParser.escape(query));
            TopDocs topDocs = searcher.search(luceneQuery, Math.min(limit * 2, candidates.size()));

            for(ScoreDoc scoreDoc : topDocs.scoreDocs){
                Document doc = searcher.doc(scoreDoc.doc);
                int index = Integer.parseInt(doc.get("index"));
                MemoryItem item = candidates.get(index);

                double timeDecay = calculateTimeDecay(item.getTimeStamp());
                double importanceWeight = 0.8 + item.getImportance() * 0.4;
                double finalScore = scoreDoc.score * timeDecay * importanceWeight;

                scoredResults.add(new ScoredMemory(item, finalScore));
            }
        }
        return scoredResults.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .map(ScoredMemory::getMemoryItem)
                .collect(Collectors.toList());

    }

    private double calculateTimeDecay(DateTime timestamp){
        if(timestamp == null) return 1.0;
        long minutesAgo = (DateTime.now().getMillis() - timestamp.getMillis()) / 60000;
        double base = Math.max(0.01, Math.min(0.999, getConfig().getDecayFactor()));
        return Math.max(0.01, Math.pow(base, minutesAgo));
    }

    @Override
    public Boolean update(String memoryId, String content, float importance, Map<String, Object> metadata) {
        clearExpireMemories();
        Optional<MemoryItem> opt = memories.stream()
                .filter(memoryItem -> memoryItem.getId().equals(memoryId))
                .findFirst();
        if(opt.isEmpty()) return false;
        MemoryItem memoryItem = opt.get();
        int oldTokens = memoryItem.getContent().length();
        if(StringUtils.isNotBlank(content)){
            memoryItem.setContent(content);
        }
        memoryItem.setImportance(importance > 0 ? importance : memoryItem.getImportance());
        if(metadata != null && !metadata.isEmpty()){
            memoryItem.setMetadata(metadata);
        }
        memoryItem.setTimeStamp(DateTime.now());
        //写回向量库
//        VectorSaveReq req = new VectorSaveReq();
//        req.setCollectionName(COLLECTION);
//        VectorSaveReq.VectorData data = new VectorSaveReq.VectorData();
//        data.setUuid(memoryItem.getId());
//        data.setEmbeddingText(memoryItem.getContent());
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("id", memoryItem.getId());
//        payload.put("content", memoryItem.getContent());
//        payload.put("importance", memoryItem.getImportance());
//        payload.put("metadata", memoryItem.getMetadata());
//        payload.put("timestamp", memoryItem.getTimeStamp().toString());
//        payload.put("memoryType", memoryItem.getMemoryType());
//        payload.put("userId", memoryItem.getUserId());
//        data.setPayloads(payload);
//        req.setDataList(Collections.singletonList(data));
//        vectorService.saveVector(req);
        //重新创建队列
        rebuildQueue();
        this.currentTokens = this.currentTokens - oldTokens + memoryItem.getContent().length();
        capacityLimits();
        return true;
    }

    @Override
    public Boolean delete(String memoryId) {
        clearExpireMemories();
        if(StringUtils.isBlank(memoryId)) return false;
        MemoryItem target = null;
        for(MemoryItem memoryItem : memories){
            if(memoryItem.getId().equals(memoryId)){
                target = memoryItem;
                break;
            }
        }
        if(target == null) return false;
        memories.remove(target);
        this.currentTokens -= target.getContent().length();
        rebuildQueue();
//        try{
//            vectorService.deleteVector(COLLECTION, Collections.singletonList(memoryId));
//        } catch (Exception e){
//            return false;
//        }
        capacityLimits();
        return true;
    }

    @Override
    public Boolean hasMemory(String memoryId) {
        return memories.stream()
                .anyMatch(memoryItem -> memoryItem.getId().equals(memoryId));
    }

    @Override
    public void clear() {

    }

    @Override
    public HashMap<String, Object> getStats() {
        return null;
    }

    private void clearExpireMemories(){
        if(memories == null || memories.isEmpty()){
            return;
        }
        DateTime cutoutTime = DateTime.now().minusMinutes(maxAgeMinutes);
        List<MemoryItem> keepMemories = new ArrayList<>();
        int removedTokenSum = 0;
        for(MemoryItem memoryItem : memories){
            if(memoryItem.getTimeStamp().isAfter(cutoutTime)){
                keepMemories.add(memoryItem);
            }else{
                removedTokenSum += memoryItem.getContent().length();
            }
        }
        if(keepMemories.size() == memories.size()) return;
        this.memories = keepMemories;
        this.currentTokens = Math.max(0, this.currentTokens - removedTokenSum);
        memoryQueue.clear();
        rebuildQueue();
        capacityLimits();
    }

    private Double calculatePriority(MemoryItem memoryItem) {
        float priority = memoryItem.getImportance();
        DateTime ts = memoryItem.getTimeStamp() != null ? memoryItem.getTimeStamp() : DateTime.now();
        long minutesAgo = Math.max(0, (DateTime.now().getMillis() - ts.getMillis()) / 60000);
        double base = Math.max(0.01, Math.min(0.999, getConfig().getDecayFactor()));
        double timeDecay = Math.pow(base, minutesAgo);
//        DateTime dateTime = DateTime.now().minusMinutes(maxAgeMinutes);
//        double hour = Double.valueOf(dateTime.getHour());
//        double decayFactor = Math.pow(this.getConfig().getDecayFactor(), hour);
//        double timeDecay =  Math.max(0.1, decayFactor);
        return priority * timeDecay;
    }

    private void capacityLimits() {
        while(this.memories.size() > this.maxCapacity){
            removeLowestPriorityMemory();
        }
        while(this.currentTokens > this.maxTokens){
            removeLowestPriorityMemory();
        }
    }

    private void removeLowestPriorityMemory() {
        if(this.memories == null || this.memories.isEmpty()) return;
        double lowestPriority = Double.MAX_VALUE;
        MemoryItem lowestPriorityMemory = null;
        for(MemoryItem memoryItem : this.memories){
            double priority = calculatePriority(memoryItem);
            if(priority < lowestPriority){
                lowestPriority = priority;
                lowestPriorityMemory = memoryItem;
            }
        }
        if(lowestPriorityMemory != null){
            delete(lowestPriorityMemory.getId());
        }
    }
    public void rebuildQueue(){
        memoryQueue.clear();
        for(MemoryItem memoryItem : memories){
            Double priority = calculatePriority(memoryItem);
            memoryQueue.add(new MemoryEntry<>(priority, memoryItem.getTimeStamp().getMillis(), memoryItem));
        }
    }
}
