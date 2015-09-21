package com.wangread.webstudy.common.db;

import com.wangread.webstudy.common.hash.Hashing;
import com.wangread.webstudy.common.util.SafeEncoder;
import com.wangread.webstudy.common.utils.IpUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yfwangrui on 2015/8/17.
 *
 * database sharded
 */
public class DataBaseSharded {
    public static final int DEFAULT_WEIGHT = 1;
    private TreeMap<Long, String> nodes;
    private final Hashing algo;

    /**
     * The default pattern used for extracting a key tag. The pattern must have a group (between
     * parenthesis), which delimits the tag to be hashed. A null pattern avoids applying the regular
     * expression for each lookup, improving performance a little bit is key tags aren't being used.
     */
    private Pattern tagPattern = null;
    // the tag is anything between {}
    public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern.compile("\\{(.+?)\\}");

    public DataBaseSharded(List<String> shards) {
        this(shards, Hashing.MURMUR_HASH); // MD5 is really not good as we works
        // with 64-bits not 128
    }

    public DataBaseSharded(List<String> shards, Hashing algo) {
        this.algo = algo;
        initialize(shards);
    }

    public DataBaseSharded(List<String> shards, Pattern tagPattern) {
        this(shards, Hashing.MURMUR_HASH, tagPattern); // MD5 is really not good
        // as we works with
        // 64-bits not 128
    }

    public DataBaseSharded(List<String> shards, Hashing algo, Pattern tagPattern) {
        this.algo = algo;
        this.tagPattern = tagPattern;
        initialize(shards);
    }

    private void initialize(List<String> shards) {
        nodes = new TreeMap<Long, String>();

        for (int i = 0; i != shards.size(); ++i) {
            final String shardInfo = shards.get(i);
            if (shardInfo == null)
                for (int n = 0; n < 160 * DEFAULT_WEIGHT; n++) {
                nodes.put(this.algo.hash("SHARD-" + i + "-NODE-" + n), null);
            }
            else for (int n = 0; n < 160 * DEFAULT_WEIGHT; n++) {
                nodes.put(this.algo.hash(shardInfo + "*" + DEFAULT_WEIGHT + n), shardInfo);
            }
        }
    }

    public String getShard() {
        return getShardInfo(IpUtils.localAddress());
    }

    public String getShardInfo(byte[] key) {
        SortedMap<Long, String> tail = nodes.tailMap(algo.hash(key));
        if (tail.isEmpty()) {
            return nodes.get(nodes.firstKey());
        }
        return tail.get(tail.firstKey());
    }

    public String getShardInfo(String key) {
        return getShardInfo(SafeEncoder.encode(getKeyTag(key)));
    }

    /**
     * A key tag is a special pattern inside a key that, if preset, is the only part of the key hashed
     * in order to select the server for this key.
     * @see <a href="http://code.google.com/p/redis/wiki/FAQ#I'm_using_some_form_of_key_hashing_for_partitioning,_but_wh"/>
     * @param key
     * @return The tag if it exists, or the original key
     */
    public String getKeyTag(String key) {
        if (tagPattern != null) {
            Matcher m = tagPattern.matcher(key);
            if (m.find()) return m.group(1);
        }
        return key;
    }

    public Collection<String> getAllShardInfo() {
        return Collections.unmodifiableCollection(nodes.values());
    }
}
