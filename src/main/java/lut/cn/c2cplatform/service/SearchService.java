package lut.cn.c2cplatform.service;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
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
     * 从Elasticsearch删除产品
     */
    public void deleteProduct(Long productId) {
        try {
            productSearchRepository.deleteById(String.valueOf(productId));
        } catch (Exception e) {
            System.err.println("Failed to delete product from ES: " + e.getMessage());
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
                        .fields("name^2", "description", "category") // name字段权重更高
                        .fuzziness("AUTO"));
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
                        .value(request.getConditionLevel()));
                filterQueries.add(Query.of(q -> q.term(termQuery)));
            }

            // 4. 位置过滤
            if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                TermQuery locationQuery = TermQuery.of(t -> t
                        .field("location")
                        .value(request.getLocation()));
                filterQueries.add(Query.of(q -> q.term(locationQuery)));
            }

            // 5. 只显示在售商品
            TermQuery statusQuery = TermQuery.of(t -> t.field("status").value(1));
            filterQueries.add(Query.of(q -> q.term(statusQuery)));

            // 6. 分类过滤
            if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                String[] categories = request.getCategory().split(",");
                if (categories.length > 1) {
                    // 如果有多个分类（逗号分隔），使用TermsQuery
                    List<String> categoryList = java.util.Arrays.asList(categories);
                    List<co.elastic.clients.elasticsearch._types.FieldValue> fieldValues = categoryList.stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                            .collect(Collectors.toList());

                    TermsQuery termsQuery = TermsQuery.of(t -> t
                            .field("category")
                            .terms(terms -> terms.value(fieldValues)));
                    filterQueries.add(Query.of(q -> q.terms(termsQuery)));
                } else {
                    // 单个分类
                    TermQuery categoryQuery = TermQuery.of(t -> t
                            .field("category")
                            .value(request.getCategory()));
                    filterQueries.add(Query.of(q -> q.term(categoryQuery)));
                }
            }

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

            // 6. 设置高亮
            HighlightParameters highlightParameters = HighlightParameters.builder()
                    .withPreTags("<em class='highlight'>")
                    .withPostTags("</em>")
                    .build();

            List<HighlightField> highlightFields = new ArrayList<>();
            highlightFields.add(new HighlightField("name"));
            highlightFields.add(new HighlightField("description"));

            Highlight highlight = new Highlight(highlightParameters, highlightFields);

            NativeQuery searchQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                    .withPageable(PageRequest.of(request.getPage(), request.getSize()))
                    .withHighlightQuery(new HighlightQuery(highlight, ProductDocument.class))
                    .build();

            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

            return searchHits.getSearchHits().stream()
                    .map(hit -> {
                        ProductDocument doc = hit.getContent();
                        List<String> nameHighlights = hit.getHighlightField("name");
                        if (nameHighlights != null && !nameHighlights.isEmpty()) {
                            doc.setHighlightedName(nameHighlights.get(0));
                        } else {
                            doc.setHighlightedName(doc.getName());
                        }

                        List<String> descHighlights = hit.getHighlightField("description");
                        if (descHighlights != null && !descHighlights.isEmpty()) {
                            doc.setHighlightedDescription(descHighlights.get(0));
                        } else {
                            doc.setHighlightedDescription(doc.getDescription());
                        }
                        return doc;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 简单的关键词搜索（用于推荐系统）
     * 
     * @param keyword 搜索关键词
     * @param limit   返回结果数量限制
     * @return 产品文档列表
     */
    public List<ProductDocument> searchProductsByKeyword(String keyword, int limit) {
        try {
            SearchRequestDTO request = new SearchRequestDTO();
            request.setKeyword(keyword);
            request.setPage(0);
            request.setSize(limit);

            return searchProducts(request);
        } catch (Exception e) {
            System.err.println("Keyword search error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 根据分类搜索商品（用于推荐系统）
     * 
     * @param category 商品分类
     * @param limit    返回结果数量限制
     * @return 产品文档列表
     */
    public List<ProductDocument> searchProductsByCategory(String category, int limit) {
        try {
            List<Query> filterQueries = new ArrayList<>();

            // 分类过滤
            TermQuery categoryQuery = TermQuery.of(t -> t
                    .field("category")
                    .value(category));
            filterQueries.add(Query.of(q -> q.term(categoryQuery)));

            // 只显示在售商品
            TermQuery statusQuery = TermQuery.of(t -> t.field("status").value(1));
            filterQueries.add(Query.of(q -> q.term(statusQuery)));

            BoolQuery boolQuery = BoolQuery.of(b -> b.filter(filterQueries));

            NativeQuery searchQuery = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.bool(boolQuery)))
                    .withPageable(PageRequest.of(0, limit))
                    .build();

            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

            return searchHits.getSearchHits().stream()
                    .map(hit -> hit.getContent())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Category search error: " + e.getMessage());
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
        doc.setCategory(product.getCategory());
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
