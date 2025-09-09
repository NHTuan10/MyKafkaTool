package io.github.nhtuan10.mykafkatool.util;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import lombok.Locked;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class PersistableConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
    private final String filePath;
    private final Kryo kryo;
//    private final  AtomicInteger counter = new AtomicInteger(0);
//    private final  ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
//    private final  Lock writeLock = readWriteLock.writeLock();
//    private final  Lock readLock = readWriteLock.readLock();
//    private final Object lock = new Object();

    public PersistableConcurrentHashMap(String filePath) {
        super();
        this.filePath = filePath;
        kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.setRegistrationRequired(false);
        try {
            Map<K, V> entries = loadFromFile();
            putAll(entries);
//            entries.forEach(entry -> put(entry.getKey(), entry.getValue()));
        } catch (NoSuchFileException e) {
            log.warn("File not found {}", filePath);
        } catch (Exception e) {
            log.warn("Error when load from file {}", filePath, e);
        }
//        Runnable writer = () -> {
//            readLock.lock();
////            if (readLock.tryLock()) {
//                try {
//                    write();
//                } catch (IOException e) {
//                    log.warn("Error when write to file", e);
//                }
//                finally {
//                    readLock.unlock();
//                }
////            }
//        };
//        Runnable writer = () -> {
//            if (counter.get() == 0) {
//                try {
//                    write();
//                } catch (IOException e) {
//                    log.warn("Error when write to file", e);
//                }
////            }
//            }
//        };

//        Runnable writer = () -> {
//            synchronized (lock){
//                try {
//                    write();
//                } catch (IOException e) {
//                    log.warn("Error when write to file", e);
//                }
//            }
//        };

//        Runnable writer = () -> {
//            try {
//                write();
//            } catch (IOException e) {
//                log.warn("Error when write to file", e);
//            }
//        };
//        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//        executor.scheduleAtFixedRate(writer, 0, 5_000, TimeUnit.MILLISECONDS);
    }


    @Override
    @Locked.Write
    public V put(K key, V value) {
//        boolean getLock = false;
//        try {
//            getLock = writeLock.tryLock(100, TimeUnit.MILLISECONDS);
//            return super.put(key, value);
//        } catch (InterruptedException e) {
//            return super.put(key, value);
//        } finally {
//            if (getLock) {
//                writeLock.unlock();
//            }
//        }
//        counter.incrementAndGet();
//        try {
//            V result = super.put(key, value);
//            return result;
//        }
//        finally {
//            counter.decrementAndGet();
//        }
//        synchronized (lock){
//            V result = super.put(key, value);
//            return result;
//        }

        V result = super.put(key, value);
        try {
            write();
        } catch (IOException e) {
            log.warn("Error when write to file {}", filePath, e);
        }
        return result;
    }

    //    @Locked.Read
    public void write() throws IOException {
        Files.write(Paths.get(filePath), serializer(new HashMap<>(this)));
    }

    public Map loadFromFile() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return deserialization(bytes, HashMap.class);
    }

    public byte[] serializer(Object obj) {
        if (obj == null)
            return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeObject(output, obj);
        output.close();
        return bos.toByteArray();
    }

    public <T> T deserialization(byte[] bytes, Class<T> type) {
        if (bytes == null)
            return null;
        kryo.register(type);
        Input input = new Input(bytes);
        T result = kryo.readObject(input, type);
        input.close();
        return result;
    }
}
