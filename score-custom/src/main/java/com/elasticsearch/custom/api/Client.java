package com.elasticsearch.custom.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import com.alibaba.fastjson.JSON;
import com.elasticsearch.custom.plugin.CustomPrefixParser;
import com.elasticsearch.custom.plugin.CustomQueryParser;
import com.elasticsearch.custom.util.DomainNames;
import com.elasticsearch.custom.util.SignatureUtil;
import com.elasticsearch.custom.util.StringUtils;

public class Client {
	private static final String CLUSTER_NAME = "lezhi";
	public static final String INDEX_PREFIX = "contentv2_";
	private static final String TYPE = "doc";
	public static final String[] fields = new String[]{"url", "title", "signature", "thumbnail", "lastModified"};
	
	protected final org.elasticsearch.client.Client client;
	
	public Client(String host) {
		this(Arrays.asList(host));
	}
	
	public Client(List<String> hosts) {
		this(CLUSTER_NAME, hosts);
	}

    public Client(String cluster, List<String> hosts) {
        this(cluster, hosts, 9300);
    }
	
	public Client(String cluster, List<String> hosts, int port) {
		 Settings settings = setNodename(ImmutableSettings.settingsBuilder())
					.put("http.enabled", "false")
					.put("transport.tcp.port", "9300-9400")
					.put("discovery.zen.ping.unicast.hosts", StringUtils.join(hosts, ",")).build();
		Node node = NodeBuilder.nodeBuilder().clusterName(cluster).client(true).settings(settings).node();
		client = node.client();
	}
	
	public String state() {
		return client.admin().cluster().state(new ClusterStateRequest()).actionGet().getState().toString();
	}
	
	public boolean updateTemplate(String name, String template) {
		PutIndexTemplateRequest request = new PutIndexTemplateRequest(name).source(template);
		return client.admin().indices().putTemplate(request).actionGet().isAcknowledged();
	}
	
	private ImmutableSettings.Builder setNodename(ImmutableSettings.Builder builder) {
        try {
            StackTraceElement[] sts = new RuntimeException().getStackTrace();
            String clazz = sts[sts.length-1].getClassName();
            String host = InetAddress.getLocalHost().getHostName();
            return builder.put("node.name", clazz + "@" + host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("could not determine hostname", e);
        }
	}

    public boolean update(String url, String field, String val) {
        String site = DomainNames.safeGetPLD(url);
        String id = SignatureUtil.signature(url);
        Map<String, String> source = new HashMap<String, String>();
        source.put(field, val);
        UpdateRequestBuilder ur = client.prepareUpdate(indexFor(site), TYPE, id);
        try {
            ur.setDoc(JSON.toJSONString(source, false)).execute().actionGet();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

	public void bulkAdd(List<Doc> docs) {
		if (docs.isEmpty()) return;
		
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (Doc doc: docs) {
			doc.title = StringUtils.escapeJson(doc.title);
			String site = DomainNames.safeGetPLD(doc.url);
			String json = JSON.toJSONString(doc, false);
			bulkRequest.add(client.prepareIndex(indexFor(site), TYPE, SignatureUtil.signature(doc.url)).setSource(json));
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            throw new RuntimeException(bulkResponse.buildFailureMessage());
        }
	}
	
	public long numDocs(String siteprefix) {
		String site = DomainNames.safeGetPLD(siteprefix);
		try {
			SearchResponse resp = client.prepareSearch(indexFor(site)).setTypes(TYPE).setSearchType(SearchType.DFS_QUERY_AND_FETCH)
					.setFilter(FilterBuilders.prefixFilter("url", siteprefix).cache(true).cacheKey(siteprefix))
					.setQuery(QueryBuilders.matchAllQuery())
					.setFrom(0).setSize(1).execute().actionGet();
			return resp.getHits().getTotalHits();
		} catch (IndexMissingException e) { // 大部分情况都是存在的
			return 0L;
		}
	}
	
	public List<String> exists(List<String> urls) {
		List<String> existUrls = new ArrayList<String>();
		if (urls.isEmpty()) return existUrls;
		
		MultiGetRequestBuilder mget = client.prepareMultiGet();
		for (String url: urls) {
			String site = DomainNames.safeGetPLD(url);
			mget.add(new MultiGetRequest.Item(indexFor(site), TYPE, SignatureUtil.signature(url)).fields("url"));
		}
		MultiGetResponse resps = mget.execute().actionGet();
		for (MultiGetItemResponse resp : resps) {
			if (resp.getResponse() != null && resp.getResponse().isExists()) {
				String url = (String) resp.getResponse().getField("url").getValue();
				existUrls.add(url);
			}
		}
		
		return existUrls;
	}
	
	public Doc get(String url) {
		String site = DomainNames.safeGetPLD(url);
		GetResponse resp = client.prepareGet(indexFor(site), TYPE, SignatureUtil.signature(url)).execute().actionGet();
		if (resp.isExists()) return fromSource(resp.getSource());
		else return null;
	}
	
	public Map<String, Doc> get(List<String> urls) {
		HashMap<String, Doc> url2docs = new HashMap<String, Doc>();
		if (urls.size() == 0) return url2docs;
		if (urls.size() == 1) {
			Doc doc = get(urls.get(0));
			if (doc != null) url2docs.put(doc.url, doc);
			return url2docs;
		}
		
		MultiGetRequestBuilder mget = client.prepareMultiGet();
		for (String url: urls) {
			String site = DomainNames.safeGetPLD(url);
			mget.add(indexFor(site), TYPE, SignatureUtil.signature(url));
		}
		MultiGetResponse resps = mget.execute().actionGet();
		for (MultiGetItemResponse resp : resps) {
			if (resp.getResponse() != null && resp.getResponse().isExists()) {
				Doc d = fromSource(resp.getResponse().getSource());
				url2docs.put(d.url, d);
			}
		}
		return url2docs;
	}

    // TODO: 抽象
    public List<HitDoc> queryThumbnail(String site, String keyword, int max) {
        Query q = new Query(null, keyword, null, System.currentTimeMillis());
        Map<String, Query> querymap = new HashMap<String, Query>();
        querymap.put(CustomQueryParser.NAME, q);

        SearchResponse resp = client.prepareSearch(indexFor(site)).setTypes(TYPE).setSearchType(SearchType.DFS_QUERY_AND_FETCH)
                .setQuery(JSON.toJSONString(querymap, false))
                .addFields("url", "thumbnail").setFrom(0).setSize(max).execute().actionGet();

        List<HitDoc> docs = new ArrayList<HitDoc>();
        for (SearchHit sh: resp.getHits()) {
            String thumbnail = sh.field("thumbnail").getValue();
            if (thumbnail.startsWith("http://")) {
                HitDoc doc = new HitDoc();
                doc.url = sh.field("url").getValue();
                doc.thumbnail = thumbnail;
                doc.score = sh.score();
                docs.add(doc);
            }
        }
        return docs;
    }
	
	public List<HitDoc> query(String siteprefix, String signature, String keyword, String topic, long refTime, int max) {
		String site = DomainNames.safeGetPLD(siteprefix);
		PrefixFilter f = new PrefixFilter(siteprefix);
		Query q = new Query(signature, keyword, topic, refTime);
		
		Map<String, Query> querymap = new HashMap<String, Query>();
		querymap.put(CustomQueryParser.NAME, q);
		Map<String, PrefixFilter> filtermap = new HashMap<String, PrefixFilter>();
		filtermap.put(CustomPrefixParser.NAME, f);
		
		SearchResponse resp = client.prepareSearch(indexFor(site)).setTypes(TYPE).setSearchType(SearchType.DFS_QUERY_AND_FETCH)
				.setFilter(JSON.toJSONString(filtermap, false)).setQuery(JSON.toJSONString(querymap, false))
				.addFields(fields).setFrom(0).setSize(max).execute().actionGet();
		
		List<HitDoc> docs = new ArrayList<HitDoc>();
//		System.out.println("total " + resp.getHits().getTotalHits() + " docs, time=" + resp.getTookInMillis());
		for (SearchHit sh: resp.getHits()) {
//			System.out.println("id: " + sh.getId() + " score: " + sh.getScore());
			HitDoc doc = fromField(sh.fields());
			doc.score = sh.getScore();
			docs.add(doc);
		}
		return docs;
	}
	
	private HitDoc fromField(Map<String, SearchHitField> fields) {
		String url = fields.get("url").getValue();
		String title = fields.get("title").getValue();
		String signature = fields.get("signature").getValue();
		String thumbnail = fields.get("thumbnail").getValue();
		long lastModified = ((Number)fields.get("lastModified").getValue()).longValue();
		return new HitDoc(url, title, signature, thumbnail, lastModified);
	}
	
	private Doc fromSource(Map<String, Object> source) {
		String url = (String)source.get("url");
		String title = (String)source.get("title");
		String signature = (String)source.get("signature");
		String thumbnail = (String)source.get("thumbnail");
		String keyword = (String)source.get("keyword");
		String topic = (String)source.get("topic");
		long lastModified = ((Number)source.get("lastModified")).longValue();
		return new Doc(url, title, signature, thumbnail, keyword, topic, lastModified);
	}
	
	private static String indexFor(String site) {
		return INDEX_PREFIX + site;
	}
	
	public void close() {
		client.close();
	}

    public static void main(String[] args) {
        Client client = new Client("192.168.1.136");
        System.out.println(client.update("http://news.m4.cn/2013-03/p1204191.shtml", "title", "abc"));
        List<HitDoc> docs = client.queryThumbnail("chinanews.com.cn", "欧洲杯|1,0,0.6428354857996375 欧洲|2,0,0.5296092278070975 新建|1,0,0.12968803015103084 工人|1,0,0.4758794976462797 乌克兰|1,0,0.7333250126122013 9.11|3,3,0.020000000000000004 足球锦标赛|1,0,0.661913698303132 9.5|3,3,0.020000000000000004 新闻|3,3,0.4039450213633012 工地|1,0,0.5652824880556966 图片|3,3,0.3679468228206871 体育场|1,0,0.6059718574373877 基辅|1,0,0.7534696325484268 精选|4,3,0.15975784390280404", 100);
        for (HitDoc doc: docs) System.out.println(doc.url + " => " + doc.thumbnail);
        client.close();
    }
}