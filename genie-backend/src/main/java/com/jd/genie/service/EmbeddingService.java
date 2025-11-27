package com.jd.genie.service;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jd.genie.agent.util.OkHttpUtil;
import com.jd.genie.config.data.DataAgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class EmbeddingService {
    @Autowired
    private DataAgentConfig dataAgentConfig;

    public List<List<Float>> getVectorBatch(List<String> text) {
//        try {
//            Map<String, String> headers = new HashMap<>();
//            headers.put("Authorization", "Bearer " + dataAgentConfig.getQdrantConfig().getEmbeddingApiKey());
//            headers.put("Content-Type", "application/json");
//            JSONObject body = new JSONObject();
//            body.put("model", dataAgentConfig.getQdrantConfig().getModel());
//            body.put("inputs", text);
//            body.put("normalize", true);
//            String res = OkHttpUtil.postJsonBody(dataAgentConfig.getQdrantConfig().getEmbeddingUrl(), headers, body.toJSONString());
//            return JSONObject.parseObject(res, new TypeReference<>() {
//            });
//        } catch (Exception e) {
//            log.error("embedding failed, error:{}", e.getMessage(), e);
//            return null;
//        }
        try {
            JSONObject body = new JSONObject();
            body.put("model", dataAgentConfig.getQdrantConfig().getModel()); // 例：text-embedding-v4
            body.put("input", text);
            body.put("dimensions", 1024);                // 如需可配，改成从配置读取
            body.put("encoding_format", "float");

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + dataAgentConfig.getQdrantConfig().getEmbeddingApiKey());
            headers.put("Content-Type", "application/json");

            String res = OkHttpUtil.postJsonBody(
                    dataAgentConfig.getQdrantConfig().getEmbeddingUrl(), headers, body.toJSONString());

            JSONObject json = JSONObject.parseObject(res);
            JSONArray data = json.getJSONArray("data");
            List<List<Float>> vectors = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JSONArray emb = data.getJSONObject(i).getJSONArray("embedding");
                vectors.add(emb.toJavaList(Float.class));
            }
            return vectors;
        } catch (Exception e) {
            log.error("embedding failed, error: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<Float> getVector(String text) {
        List<List<Float>> vectorBatch = getVectorBatch(Collections.singletonList(text));
        if (CollectionUtils.isNotEmpty(vectorBatch)) {
            return vectorBatch.get(0);
        }
        return null;
    }
}
