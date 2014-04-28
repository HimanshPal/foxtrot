package com.flipkart.foxtrot.core.querystore.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.foxtrot.common.ActionResponse;
import com.flipkart.foxtrot.common.Document;
import com.flipkart.foxtrot.common.query.Filter;
import com.flipkart.foxtrot.common.query.FilterCombinerType;
import com.flipkart.foxtrot.common.query.Query;
import com.flipkart.foxtrot.common.query.ResultSort;
import com.flipkart.foxtrot.common.query.general.AnyFilter;
import com.flipkart.foxtrot.common.query.general.EqualsFilter;
import com.flipkart.foxtrot.common.query.general.NotEqualsFilter;
import com.flipkart.foxtrot.common.query.numeric.*;
import com.flipkart.foxtrot.common.query.string.ContainsFilter;
import com.flipkart.foxtrot.core.MockElasticsearchServer;
import com.flipkart.foxtrot.core.TestUtils;
import com.flipkart.foxtrot.core.common.AsyncDataToken;
import com.flipkart.foxtrot.core.common.CacheUtils;
import com.flipkart.foxtrot.core.datastore.DataStore;
import com.flipkart.foxtrot.core.querystore.QueryExecutor;
import com.flipkart.foxtrot.core.querystore.QueryStoreException;
import com.flipkart.foxtrot.core.querystore.TableMetadataManager;
import com.flipkart.foxtrot.core.querystore.actions.spi.AnalyticsLoader;
import com.flipkart.foxtrot.core.querystore.impl.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Created by rishabh.goyal on 28/04/14.
 */
public class FilterActionTest {
    private static final Logger logger = LoggerFactory.getLogger(FilterActionTest.class.getSimpleName());
    private static QueryExecutor queryExecutor;
    private static ObjectMapper mapper = new ObjectMapper();
    private static MockElasticsearchServer elasticsearchServer = new MockElasticsearchServer();
    private static HazelcastInstance hazelcastInstance;
    private static String TEST_APP = "test-app";

    @BeforeClass
    public static void setUp() throws Exception {
        ElasticsearchUtils.setMapper(mapper);
        DataStore dataStore = TestUtils.getDataStore();

        //Initializing Cache Factory
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        HazelcastConnection hazelcastConnection = Mockito.mock(HazelcastConnection.class);
        when(hazelcastConnection.getHazelcast()).thenReturn(hazelcastInstance);
        CacheUtils.setCacheFactory(new DistributedCacheFactory(hazelcastConnection, mapper));

        ElasticsearchConnection elasticsearchConnection = Mockito.mock(ElasticsearchConnection.class);
        when(elasticsearchConnection.getClient()).thenReturn(elasticsearchServer.getClient());
        ElasticsearchUtils.initializeMappings(elasticsearchServer.getClient());

        // Ensure that table exists before saving/reading data from it
        TableMetadataManager tableMetadataManager = Mockito.mock(TableMetadataManager.class);
        when(tableMetadataManager.exists(TEST_APP)).thenReturn(true);

        AnalyticsLoader analyticsLoader = new AnalyticsLoader(dataStore, elasticsearchConnection);
        TestUtils.registerActions(analyticsLoader, mapper);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        queryExecutor = new QueryExecutor(analyticsLoader, executorService);
        new ElasticsearchQueryStore(tableMetadataManager, elasticsearchConnection, dataStore, queryExecutor)
                .save(TEST_APP, getQueryDocuments());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        elasticsearchServer.shutdown();
        hazelcastInstance.shutdown();
    }

    @Test
    public void testQueryNoFilterAscending() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - No Filter - Sort Ascending");
        Query query = new Query();
        query.setTable(TEST_APP);
        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.asc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);
        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}," +
                "{\"id\":\"B\",\"timestamp\":1397658118001,\"data\":{\"os\":\"android\",\"device\":\"galaxy\",\"version\":1}}," +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}," +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}" +
                "]}";
        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - No Filter - Sort Ascending");
    }

    @Test
    public void testQueryNoFilterAscendingAsync() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - No Filter - Sort Ascending - Async");
        Query query = new Query();
        query.setTable(TEST_APP);
        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.asc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);
        AsyncDataToken response = queryExecutor.executeAsync(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}," +
                "{\"id\":\"B\",\"timestamp\":1397658118001,\"data\":{\"os\":\"android\",\"device\":\"galaxy\",\"version\":1}}," +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}," +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}" +
                "]}";
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ActionResponse actionResponse = CacheUtils.getCacheFor(response.getAction()).get(response.getKey());
        String actualResponse = mapper.writeValueAsString(actionResponse);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - No Filter - Sort Ascending - Async");
    }

    @Test
    public void testQueryNoFilterDescending() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - No Filter - Sort Descending");
        Query query = new Query();
        query.setTable(TEST_APP);
        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);
        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}," +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"B\",\"timestamp\":1397658118001,\"data\":{\"os\":\"android\",\"device\":\"galaxy\",\"version\":1}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}," +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - No Filter - Sort Descending");
    }

    @Test
    public void testQueryNoFilterWithLimit() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - No Filter - Limit 2");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(2);
        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);
        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - No Filter - Limit 2");
    }

    @Test
    public void testQueryAnyFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - Any Filter");
        Query query = new Query();
        query.setTable(TEST_APP);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        AnyFilter filter = new AnyFilter();
        query.setFilters(Collections.<Filter>singletonList(filter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}," +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"B\",\"timestamp\":1397658118001,\"data\":{\"os\":\"android\",\"device\":\"galaxy\",\"version\":1}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}," +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - Any Filter");
    }

    @Test
    public void testQueryEqualsFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - equals Filter");
        Query query = new Query();
        query.setTable(TEST_APP);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        EqualsFilter equalsFilter = new EqualsFilter();
        equalsFilter.setField("os");
        equalsFilter.setValue("ios");
        query.setFilters(Collections.<Filter>singletonList(equalsFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - equals Filter");
    }

    @Test
    public void testQueryNotEqualsFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - not_equals Filter");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(3);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        NotEqualsFilter notEqualsFilter = new NotEqualsFilter();
        notEqualsFilter.setField("os");
        notEqualsFilter.setValue("ios");
        query.setFilters(Collections.<Filter>singletonList(notEqualsFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"B\",\"timestamp\":1397658118001,\"data\":{\"os\":\"android\",\"device\":\"galaxy\",\"version\":1}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - not_equals Filter");
    }

    @Test
    public void testQueryGreaterThanFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - greater_than Filter");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(3);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        GreaterThanFilter greaterThanFilter = new GreaterThanFilter();
        greaterThanFilter.setField("battery");
        greaterThanFilter.setValue(48);
        query.setFilters(Collections.<Filter>singletonList(greaterThanFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - greater_than Filter");
    }

    @Test
    public void testQueryGreaterEqualFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - greater_equal Filter");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(3);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        GreaterEqualFilter greaterEqualFilter = new GreaterEqualFilter();
        greaterEqualFilter.setField("battery");
        greaterEqualFilter.setValue(48);
        query.setFilters(Collections.<Filter>singletonList(greaterEqualFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - greater_equal Filter");
    }

    @Test
    public void testQueryLessThanFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - less_than Filter");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(3);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        LessThanFilter lessThanFilter = new LessThanFilter();
        lessThanFilter.setField("battery");
        lessThanFilter.setValue(48);
        query.setFilters(Collections.<Filter>singletonList(lessThanFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - less_than Filter");
    }

    @Test
    public void testQueryLessEqualFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - greater_equal Filter");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(3);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        LessEqualFilter lessEqualFilter = new LessEqualFilter();
        lessEqualFilter.setField("battery");
        lessEqualFilter.setValue(48);
        query.setFilters(Collections.<Filter>singletonList(lessEqualFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - greater_equal Filter");
    }

    @Test
    public void testQueryBetweenFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - between Filter");
        Query query = new Query();
        query.setTable(TEST_APP);
        query.setLimit(3);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        BetweenFilter betweenFilter = new BetweenFilter();
        betweenFilter.setField("battery");
        betweenFilter.setFrom(47);
        betweenFilter.setTo(75);
        query.setFilters(Collections.<Filter>singletonList(betweenFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - between Filter");
    }

    @Test
    public void testQueryContainsFilter() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - contains Filter");
        Query query = new Query();
        query.setTable(TEST_APP);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        ContainsFilter containsFilter = new ContainsFilter();
        containsFilter.setField("os");
        containsFilter.setExpression(".*droid.*");
        query.setFilters(Collections.<Filter>singletonList(containsFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"B\",\"timestamp\":1397658118001,\"data\":{\"os\":\"android\",\"device\":\"galaxy\",\"version\":1}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}," +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - contains Filter");
    }

    @Test
    public void testQueryEmptyResult() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - Empty Result Test");
        Query query = new Query();
        query.setTable(TEST_APP);

        EqualsFilter equalsFilter = new EqualsFilter();
        equalsFilter.setField("os");
        equalsFilter.setValue("wp8");
        query.setFilters(Collections.<Filter>singletonList(equalsFilter));

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - Empty Result Test");
    }

    @Test
    public void testQueryMultipleFiltersEmptyResult() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - Multiple Filters - Empty Result");
        Query query = new Query();
        query.setTable(TEST_APP);

        EqualsFilter equalsFilter = new EqualsFilter();
        equalsFilter.setField("os");
        equalsFilter.setValue("android");

        GreaterEqualFilter greaterEqualFilter = new GreaterEqualFilter();
        greaterEqualFilter.setField("battery");
        greaterEqualFilter.setValue(100);

        List<Filter> filters = new Vector<Filter>();
        filters.add(equalsFilter);
        filters.add(greaterEqualFilter);
        query.setFilters(filters);

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - Multiple Filters - Empty Result");
    }

    @Test
    public void testQueryMultipleFiltersAndCombiner() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - Multiple Filters - Non Empty Result");
        Query query = new Query();
        query.setTable(TEST_APP);

        EqualsFilter equalsFilter = new EqualsFilter();
        equalsFilter.setField("os");
        equalsFilter.setValue("android");

        GreaterEqualFilter greaterEqualFilter = new GreaterEqualFilter();
        greaterEqualFilter.setField("battery");
        greaterEqualFilter.setValue(98);

        List<Filter> filters = new Vector<Filter>();
        filters.add(equalsFilter);
        filters.add(greaterEqualFilter);
        query.setFilters(filters);

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - Multiple Filters - Non Empty Result");
    }

    @Test
    public void testQueryMultipleFiltersOrCombiner() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - Multiple Filters - Or Combiner - Non Empty Result");
        Query query = new Query();
        query.setTable(TEST_APP);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        EqualsFilter equalsFilter = new EqualsFilter();
        equalsFilter.setField("os");
        equalsFilter.setValue("ios");

        EqualsFilter equalsFilter2 = new EqualsFilter();
        equalsFilter2.setField("device");
        equalsFilter2.setValue("nexus");

        List<Filter> filters = new Vector<Filter>();
        filters.add(equalsFilter);
        filters.add(equalsFilter2);
        query.setFilters(filters);

        query.setCombiner(FilterCombinerType.or);

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"E\",\"timestamp\":1397658118004,\"data\":{\"os\":\"ios\",\"device\":\"ipad\",\"version\":2}}," +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}," +
                "{\"id\":\"C\",\"timestamp\":1397658118002,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":2}}," +
                "{\"id\":\"A\",\"timestamp\":1397658118000,\"data\":{\"os\":\"android\",\"device\":\"nexus\",\"version\":1}}," +
                "{\"id\":\"W\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":99,\"device\":\"nexus\"}}," +
                "{\"id\":\"X\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":74,\"device\":\"nexus\"}}," +
                "{\"id\":\"Y\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":48,\"device\":\"nexus\"}}," +
                "{\"id\":\"Z\",\"timestamp\":1397658117000,\"data\":{\"os\":\"android\",\"battery\":24,\"device\":\"nexus\"}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - Multiple Filters - Or Combiner - Non Empty Result");
    }

    @Test
    public void testQueryPagination() throws QueryStoreException, JsonProcessingException {
        logger.info("Testing Query - Filter with Pagination");
        Query query = new Query();
        query.setTable(TEST_APP);

        ResultSort resultSort = new ResultSort();
        resultSort.setOrder(ResultSort.Order.desc);
        resultSort.setField("_timestamp");
        query.setSort(resultSort);

        EqualsFilter equalsFilter = new EqualsFilter();
        equalsFilter.setField("os");
        equalsFilter.setValue("ios");
        query.setFilters(Collections.<Filter>singletonList(equalsFilter));

        query.setFrom(1);
        query.setLimit(1);

        ActionResponse response = queryExecutor.execute(query);
        String expectedResponse = "{\"opcode\":\"query\",\"documents\":[" +
                "{\"id\":\"D\",\"timestamp\":1397658118003,\"data\":{\"os\":\"ios\",\"device\":\"iphone\",\"version\":1}}" +
                "]}";

        String actualResponse = mapper.writeValueAsString(response);
        assertEquals(expectedResponse, actualResponse);
        logger.info("Tested Query - Filter with Pagination");
    }

    private static List<Document> getQueryDocuments() {
        List<Document> documents = new Vector<Document>();
        documents.add(TestUtils.getDocument("Z", 1397658117000L, new Object[]{"os", "android", "device", "nexus", "battery", 24}, mapper));
        documents.add(TestUtils.getDocument("Y", 1397658117000L, new Object[]{"os", "android", "device", "nexus", "battery", 48}, mapper));
        documents.add(TestUtils.getDocument("X", 1397658117000L, new Object[]{"os", "android", "device", "nexus", "battery", 74}, mapper));
        documents.add(TestUtils.getDocument("W", 1397658117000L, new Object[]{"os", "android", "device", "nexus", "battery", 99}, mapper));
        documents.add(TestUtils.getDocument("A", 1397658118000L, new Object[]{"os", "android", "version", 1, "device", "nexus"}, mapper));
        documents.add(TestUtils.getDocument("B", 1397658118001L, new Object[]{"os", "android", "version", 1, "device", "galaxy"}, mapper));
        documents.add(TestUtils.getDocument("C", 1397658118002L, new Object[]{"os", "android", "version", 2, "device", "nexus"}, mapper));
        documents.add(TestUtils.getDocument("D", 1397658118003L, new Object[]{"os", "ios", "version", 1, "device", "iphone"}, mapper));
        documents.add(TestUtils.getDocument("E", 1397658118004L, new Object[]{"os", "ios", "version", 2, "device", "ipad"}, mapper));
        return documents;
    }
}
