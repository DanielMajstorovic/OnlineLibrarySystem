package org.unibl.etf.mdp.redis;

import java.util.logging.Level;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {

    private static RedisManager instance;
    private JedisPool pool;

    private RedisManager() {
        try {
            String host = ConfigLoader.getProperty("redis.host");
            int port = Integer.parseInt(ConfigLoader.getProperty("redis.port"));
            String pass = ConfigLoader.getProperty("redis.pass");

            AppLogger.getLogger().info("Povezivanje na Redis " + host + ":" + port);

            if(pass != null && pass.trim().length() > 0) {
                pool = new JedisPool(new JedisPoolConfig(), host, port, 2000, pass);
            } else {
                pool = new JedisPool(new JedisPoolConfig(), host, port);
            }

        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "Greška pri inicijalizaciji Redis konekcije:", e);
        }
    }

    public static synchronized RedisManager getInstance() {
        if (instance == null) {
            instance = new RedisManager();
        }
        return instance;
    }

    public Jedis getJedis() {
        return pool.getResource();
    }
}
