package lut.cn.c2cplatform.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import lut.cn.c2cplatform.document.ProductDocument;
import lut.cn.c2cplatform.dto.SearchRequestDTO;
import lut.cn.c2cplatform.entity.Product;
import lut.cn.c2cplatform.repository.ProductSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ProductService productService;

    /**
     * 同步产品到Elasticsearch（双写模式）
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument doc = convertToDocument(product);
            productSearchRepository.save(doc);
        } catch (Exception e) {
            // 记录日志，但不影响主流程
            System.err.println("Failed to index product: " + e.getMessage());
        }
    }

    /**
     * 搜索商品
     */
    public List<ProductDocument> searchProducts(SearchRequestDTO request) {
        try {
            List<Query> mustQueries = new ArrayList<>();
            List<Query> filterQueries = new ArrayList<>();

            // 1. 全文搜索查询（如果有关键词）
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(request.getKeyword())
                    .fields("name^2", "description") // name字段权重更高
                );
                mustQueries.add(Query.of(q -> q.multiMatch(multiMatchQuery)));
            }

            // 2. 价格区间过滤
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                RangeQuery.Builder rangeBuilder = new RangeQuery.Builder().field("price");
                if (request.getMinPrice() != null) {
                    rangeBuilder.gte(co.elastic.clients.json.JsonData.of(request.getMinPrice().doubleValue()));
                }
                if (request.getMaxPrice() != null) {
                    rangeBuilder.lte(co.elastic.clients.json.JsonData.of(request.getMaxPrice().doubleValue()));
                }
                filterQueries.add(Query.of(q -> q.range(rangeBuilder.build())));
            }

            // 3. 成色过滤
            if (request.getConditionLevel() != null) {
                TermQuery termQuery = TermQuery.of(t -> t
                    .field("conditionLevel")
                    .value(request.getConditionLevel())
                );
                filterQueries.add(Query.of(q -> q.term(termQuery)));
            }

            // 4. 位置过滤
            if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                TermQuery locationQuery = TermQuery.of(t -> t
                    .field("location")
                    .value(request.getLocation())
                );
                filterQueries.add(Query.of(q -> q.term(locationQuery)));
            }

            // 5. 只显示在售商品
            TermQuery statusQuery = TermQuery.of(t -> t.field("status").value(1));
            filterQueries.add(Query.of(q -> q.term(statusQuery)));

            // 构建布尔查询
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            if (!mustQueries.isEmpty()) {
                boolQueryBuilder.must(mustQueries);
            }
            if (!filterQueries.isEmpty()) {
                boolQueryBuilder.filter(filterQueries);
            }

            // 如果没有任何查询条件，返回所有在售商品
            if (mustQueries.isEmpty() && filterQueries.size() == 1) {
                boolQueryBuilder.must(Query.of(q -> q.matchAll(MatchAllQuery.of(m -> m))));
            }

            NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(PageRequest.of(request.getPage(), request.getSize()))
                .build();

            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

            return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 将Product实体转换为Elasticsearch文档
     */
    private ProductDocument convertToDocument(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(String.valueOf(product.getId()));
        doc.setProductId(Long.valueOf(product.getId()));
        doc.setUserId(product.getUserId());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setPrice(product.getPrice());
        doc.setConditionLevel(product.getConditionLevel());
        doc.setLocation(product.getLocation());
        doc.setStatus(product.getStatus());
        doc.setCreatedAt(product.getCreatedAt());
        return doc;
    }

    /**
     * 全量同步：将所有产品同步到ES
     */
    public void syncAllProducts() {
        try {
            List<Product> allProducts = productService.listAllProducts();
            List<ProductDocument> documents = allProducts.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
            productSearchRepository.saveAll(documents);
            System.out.println("Synced " + documents.size() + " products to Elasticsearch");
        } catch (Exception e) {
            System.err.println("Failed to sync products: " + e.getMessage());
        }
    }
}

