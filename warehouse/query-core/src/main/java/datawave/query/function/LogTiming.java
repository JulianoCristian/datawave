package datawave.query.function;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import datawave.query.attributes.Numeric;
import datawave.query.iterator.profile.QuerySpan;
import datawave.query.attributes.Document;
import datawave.query.attributes.TimingMetadata;

import org.apache.accumulo.core.data.Key;

import com.google.common.base.Function;
import org.apache.log4j.Logger;

/**
 * Updates the timing information per document
 */
public class LogTiming implements Function<Entry<Key,Document>,Entry<Key,Document>> {
    
    public static final String TIMING_METADATA = "TIMING_METADATA";
    protected QuerySpan spanRunner;
    static private String host = null;
    static private Logger log = Logger.getLogger(QuerySpan.class);
    
    static {
        try {
            host = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
    }
    
    public LogTiming(QuerySpan spanRunner) {
        this.spanRunner = spanRunner;
    }
    
    @Override
    public Entry<Key,Document> apply(Entry<Key,Document> entry) {
        
        addTimingMetadata(entry.getValue(), this.spanRunner);
        return entry;
    }
    
    public static void addTimingMetadata(Document document, QuerySpan querySpan) {
        
        if (document != null && querySpan != null) {
            TimingMetadata timingMetadata = new TimingMetadata();
            synchronized (querySpan) {
                timingMetadata.setHost(host);
                timingMetadata.setSourceCount(querySpan.getSourceCount());
                timingMetadata.setSeekCount(querySpan.getSeekCount());
                timingMetadata.setNextCount(querySpan.getNextCount());
                long totalStageTimers = querySpan.getStageTimerTotal();
                // do not report timers that are less than 5% of the total
                double threshold = totalStageTimers * 0.05;
                for (Entry<String,Long> e : querySpan.getStageTimers().entrySet()) {
                    if (e.getValue().longValue() >= threshold) {
                        timingMetadata.addStageTimer(e.getKey(), new Numeric(e.getValue(), document.getMetadata(), document.isToKeep()));
                    }
                }
                querySpan.reset();
            }
            document.put(TIMING_METADATA, timingMetadata);
        }
    }
}
