package org.xhy.infrastructure.rag.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Milvus向量存储服务 */
@Service
public class MilvusVectorStore {

    private static final Logger logger = LoggerFactory.getLogger(MilvusVectorStore.class);
    private static final String COLLECTION_NAME = "paper_chunks";
    private static final int VECTOR_DIM = 1536; // 默认维度

    private final MilvusServiceClient milvusClient;

    public MilvusVectorStore(MilvusServiceClient milvusClient) {
        this.milvusClient = milvusClient;
        initCollection();
    }

    /** 初始化Collection */
    private void initCollection() {
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME).build();

        if (milvusClient.hasCollection(hasParam).getData()) {
            logger.info("Milvus Collection已存在: {}", COLLECTION_NAME);
            return;
        }

        FieldType idField = FieldType.newBuilder()
                .withName("id").withDataType(DataType.Int64).withPrimaryKey(true).withAutoID(true).build();
        FieldType chunkIdField = FieldType.newBuilder()
                .withName("chunk_id").withDataType(DataType.VarChar).withMaxLength(128).build();
        FieldType paperIdField = FieldType.newBuilder()
                .withName("paper_id").withDataType(DataType.VarChar).withMaxLength(64).build();
        FieldType textField = FieldType.newBuilder()
                .withName("text").withDataType(DataType.VarChar).withMaxLength(65535).build();
        FieldType vectorField = FieldType.newBuilder()
                .withName("embedding").withDataType(DataType.FloatVector).withDimension(VECTOR_DIM).build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDescription("论文文档分块向量存储")
                .addFieldType(idField).addFieldType(chunkIdField)
                .addFieldType(paperIdField).addFieldType(textField)
                .addFieldType(vectorField)
                .build();

        milvusClient.createCollection(createParam);
        logger.info("Milvus Collection创建成功: {}, 维度: {}", COLLECTION_NAME, VECTOR_DIM);
    }

    /** 插入向量 */
    public void insertVectors(String paperId, List<String> chunkIds, List<String> texts, List<List<Float>> vectors) {
        if (chunkIds.isEmpty()) return;

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("chunk_id", chunkIds));
        fields.add(new InsertParam.Field("paper_id", Collections.nCopies(chunkIds.size(), paperId)));
        fields.add(new InsertParam.Field("text", texts));
        fields.add(new InsertParam.Field("embedding", vectors));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME).withFields(fields).build();

        milvusClient.insert(insertParam);
        logger.info("向量插入成功: {}条, paperId={}", chunkIds.size(), paperId);
    }

    /** 向量检索 */
    public List<String> search(List<Float> queryVector, int topK) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withFloatVectors(Collections.singletonList(queryVector))
                .withVectorFieldName("embedding")
                .withParams("{\"nprobe\":10}")
                .addOutField("chunk_id").addOutField("text").addOutField("paper_id")
                .build();

        R<SearchResults> results = milvusClient.search(searchParam);
        // 提取检索到的文本
        List<String> retrievedTexts = new ArrayList<>();
        // TODO: 解析SearchResults获取实际文本
        logger.info("向量检索完成, topK={}", topK);
        return retrievedTexts;
    }
}
